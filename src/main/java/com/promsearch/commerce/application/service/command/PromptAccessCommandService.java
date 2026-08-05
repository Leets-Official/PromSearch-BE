package com.promsearch.commerce.application.service.command;

import com.promsearch.commerce.application.port.out.copy.CheckPostCopyPort;
import com.promsearch.commerce.application.port.out.copy.SavePostCopyPort;
import com.promsearch.commerce.application.port.out.prompt.IncreasePromptCopyCountPort;
import com.promsearch.commerce.application.port.out.prompt.LoadPromptAccessTargetPort;
import com.promsearch.commerce.application.port.out.prompt.LoadPromptAccessTargetPort.PromptAccessTarget;
import com.promsearch.commerce.application.port.out.unlock.CheckPostUnlockPort;
import com.promsearch.commerce.application.port.out.unlock.SavePostUnlockPort;
import com.promsearch.commerce.application.usecase.CopyPromptUseCase;
import com.promsearch.commerce.application.usecase.UnlockPromptUseCase;
import com.promsearch.commerce.application.usecase.dto.CopyPromptCommand;
import com.promsearch.commerce.application.usecase.dto.CopyPromptInfo;
import com.promsearch.commerce.application.usecase.dto.UnlockPromptCommand;
import com.promsearch.commerce.domain.PostCopy;
import com.promsearch.commerce.domain.PostUnlock;
import com.promsearch.commerce.domain.exception.CommerceDomainException;
import com.promsearch.commerce.domain.exception.CommerceErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class PromptAccessCommandService implements UnlockPromptUseCase, CopyPromptUseCase {

    private static final long MVP_PAID_POINT = 0L;
    private static final long MVP_CREATOR_REWARD_POINT = 0L;

    private final LoadPromptAccessTargetPort loadPromptAccessTargetPort;
    private final CheckPostUnlockPort checkPostUnlockPort;
    private final SavePostUnlockPort savePostUnlockPort;
    private final CheckPostCopyPort checkPostCopyPort;
    private final SavePostCopyPort savePostCopyPort;
    private final IncreasePromptCopyCountPort increasePromptCopyCountPort;

    @Override
    public void unlock(UnlockPromptCommand command) {
        PromptAccessTarget target = loadPromptAccessTargetPort.getByIdForUpdate(command.promptId());
        if (isFree(target) || isAuthor(command.userId(), target)) {
            return;
        }
        if (checkPostUnlockPort.isUnlocked(command.userId(), command.promptId())) {
            return;
        }

        savePostUnlockPort.save(PostUnlock.create(
                command.promptId(),
                command.userId(),
                target.authorId(),
                MVP_PAID_POINT,
                MVP_CREATOR_REWARD_POINT
        ));
    }

    @Override
    public CopyPromptInfo copy(CopyPromptCommand command) {
        PromptAccessTarget target = loadPromptAccessTargetPort.getByIdForUpdate(command.promptId());
        boolean author = isAuthor(command.userId(), target);
        if (!isFree(target)
                && !author
                && !checkPostUnlockPort.isUnlocked(command.userId(), command.promptId())) {
            throw new CommerceDomainException(CommerceErrorCode.PAID_PROMPT_ACCESS_DENIED);
        }

        boolean newlyCounted = false;
        if (!author && !checkPostCopyPort.isCopied(command.userId(), command.promptId())) {
            savePostCopyPort.save(PostCopy.create(command.promptId(), command.userId()));
            increasePromptCopyCountPort.increase(command.promptId());
            newlyCounted = true;
        }
        return new CopyPromptInfo(
                command.promptId(),
                newlyCounted ? Math.incrementExact(target.copyCount()) : target.copyCount(),
                newlyCounted
        );
    }

    private boolean isFree(PromptAccessTarget target) {
        return target.free();
    }

    private boolean isAuthor(Long userId, PromptAccessTarget target) {
        return userId.equals(target.authorId());
    }
}
