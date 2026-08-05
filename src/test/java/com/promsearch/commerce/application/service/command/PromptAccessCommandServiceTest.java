package com.promsearch.commerce.application.service.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.promsearch.commerce.application.port.out.copy.CheckPostCopyPort;
import com.promsearch.commerce.application.port.out.copy.SavePostCopyPort;
import com.promsearch.commerce.application.port.out.prompt.IncreasePromptCopyCountPort;
import com.promsearch.commerce.application.port.out.prompt.LoadPromptAccessTargetPort;
import com.promsearch.commerce.application.port.out.prompt.LoadPromptAccessTargetPort.PromptAccessTarget;
import com.promsearch.commerce.application.port.out.unlock.CheckPostUnlockPort;
import com.promsearch.commerce.application.port.out.unlock.SavePostUnlockPort;
import com.promsearch.commerce.application.usecase.dto.CopyPromptCommand;
import com.promsearch.commerce.application.usecase.dto.CopyPromptInfo;
import com.promsearch.commerce.application.usecase.dto.UnlockPromptCommand;
import com.promsearch.commerce.domain.PostCopy;
import com.promsearch.commerce.domain.PostUnlock;
import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PromptAccessCommandServiceTest {

    @Mock LoadPromptAccessTargetPort loadPromptAccessTargetPort;
    @Mock CheckPostUnlockPort checkPostUnlockPort;
    @Mock SavePostUnlockPort savePostUnlockPort;
    @Mock CheckPostCopyPort checkPostCopyPort;
    @Mock SavePostCopyPort savePostCopyPort;
    @Mock IncreasePromptCopyCountPort increasePromptCopyCountPort;

    private PromptAccessCommandService service;

    @BeforeEach
    void setUp() {
        service = new PromptAccessCommandService(
                loadPromptAccessTargetPort,
                checkPostUnlockPort,
                savePostUnlockPort,
                checkPostCopyPort,
                savePostCopyPort,
                increasePromptCopyCountPort
        );
    }

    @Test
    void unlocksPremiumPromptWithoutPointTransfer() {
        when(loadPromptAccessTargetPort.getByIdForUpdate(10L)).thenReturn(premiumTarget());

        service.unlock(new UnlockPromptCommand(2L, 10L));

        ArgumentCaptor<PostUnlock> captor = ArgumentCaptor.forClass(PostUnlock.class);
        verify(savePostUnlockPort).save(captor.capture());
        assertThat(captor.getValue().getPaidPoint()).isZero();
        assertThat(captor.getValue().getCreatorRewardPoint()).isZero();
    }

    @Test
    void duplicateUnlockIsIdempotent() {
        when(loadPromptAccessTargetPort.getByIdForUpdate(10L)).thenReturn(premiumTarget());
        when(checkPostUnlockPort.isUnlocked(2L, 10L)).thenReturn(true);

        service.unlock(new UnlockPromptCommand(2L, 10L));

        verify(savePostUnlockPort, never()).save(any(PostUnlock.class));
    }

    @Test
    void freePromptAndAuthorRequireNoUnlockRecord() {
        when(loadPromptAccessTargetPort.getByIdForUpdate(10L)).thenReturn(freeTarget());
        when(loadPromptAccessTargetPort.getByIdForUpdate(11L)).thenReturn(
                new PromptAccessTarget(11L, 1L, false, 0L));

        service.unlock(new UnlockPromptCommand(2L, 10L));
        service.unlock(new UnlockPromptCommand(1L, 11L));
        verify(savePostUnlockPort, never()).save(any(PostUnlock.class));
    }

    @Test
    void rejectsCopyOfLockedPremiumPrompt() {
        when(loadPromptAccessTargetPort.getByIdForUpdate(10L)).thenReturn(premiumTarget());

        assertThatThrownBy(() -> service.copy(new CopyPromptCommand(2L, 10L)))
                .isInstanceOf(CommerceDomainException.class)
                .extracting("baseCode")
                .isEqualTo(CommerceErrorCode.PAID_PROMPT_ACCESS_DENIED);

        verify(savePostCopyPort, never()).save(any(PostCopy.class));
        verify(increasePromptCopyCountPort, never()).increase(any());
    }

    @Test
    void firstAuthorizedCopyReturnsIncrementedCount() {
        when(loadPromptAccessTargetPort.getByIdForUpdate(10L)).thenReturn(premiumTarget(15L));
        when(checkPostUnlockPort.isUnlocked(2L, 10L)).thenReturn(true);

        CopyPromptInfo result = service.copy(new CopyPromptCommand(2L, 10L));

        assertThat(result).isEqualTo(new CopyPromptInfo(10L, 16L, true));
        verify(savePostCopyPort).save(any(PostCopy.class));
        verify(increasePromptCopyCountPort).increase(10L);
    }

    @Test
    void duplicateCopyReturnsCurrentCountWithoutIncreasingCount() {
        when(loadPromptAccessTargetPort.getByIdForUpdate(10L)).thenReturn(freeTarget(15L));
        when(checkPostCopyPort.isCopied(2L, 10L)).thenReturn(true);

        CopyPromptInfo result = service.copy(new CopyPromptCommand(2L, 10L));

        assertThat(result).isEqualTo(new CopyPromptInfo(10L, 15L, false));
        verify(savePostCopyPort, never()).save(any(PostCopy.class));
        verify(increasePromptCopyCountPort, never()).increase(any());
    }

    @Test
    void authorCopyIsNotRecordedOrCounted() {
        when(loadPromptAccessTargetPort.getByIdForUpdate(10L)).thenReturn(premiumTarget(15L));

        CopyPromptInfo result = service.copy(new CopyPromptCommand(1L, 10L));

        assertThat(result.copyCount()).isEqualTo(15L);
        assertThat(result.newlyCounted()).isFalse();
        verify(savePostCopyPort, never()).save(any(PostCopy.class));
        verify(increasePromptCopyCountPort, never()).increase(any());
    }

    private PromptAccessTarget premiumTarget() {
        return premiumTarget(0L);
    }

    private PromptAccessTarget premiumTarget(long copyCount) {
        return new PromptAccessTarget(10L, 1L, false, copyCount);
    }

    private PromptAccessTarget freeTarget() {
        return freeTarget(0L);
    }

    private PromptAccessTarget freeTarget(long copyCount) {
        return new PromptAccessTarget(10L, 1L, true, copyCount);
    }
}
