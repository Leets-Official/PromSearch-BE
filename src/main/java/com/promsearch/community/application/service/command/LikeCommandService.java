package com.promsearch.community.application.service.command;

import com.promsearch.community.application.port.out.postinteraction.CreatePostInteractionPort;
import com.promsearch.community.application.port.out.postinteraction.DeletePostInteractionPort;
import com.promsearch.community.application.port.out.prompt.UpdatePromptLikeCountPort;
import com.promsearch.community.application.usecase.LikePromptUseCase;
import com.promsearch.community.application.usecase.UnlikePromptUseCase;
import com.promsearch.community.application.usecase.dto.LikeInfo;
import com.promsearch.community.application.usecase.dto.LikePromptCommand;
import com.promsearch.community.domain.PostInteraction;
import com.promsearch.community.domain.enums.InteractionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LikeCommandService implements LikePromptUseCase, UnlikePromptUseCase {

    private final CreatePostInteractionPort createPostInteractionPort;
    private final DeletePostInteractionPort deletePostInteractionPort;
    private final UpdatePromptLikeCountPort updatePromptLikeCountPort;

    @Override
    public LikeInfo like(LikePromptCommand command) {
        createPostInteractionPort.create(PostInteraction.create(
                command.userId(),
                command.promptId(),
                InteractionType.LIKE
        ));
        long likeCount = updatePromptLikeCountPort.increase(command.promptId());
        return new LikeInfo(command.promptId(), true, likeCount);
    }

    @Override
    public LikeInfo unlike(LikePromptCommand command) {
        deletePostInteractionPort.delete(
                command.userId(),
                command.promptId(),
                InteractionType.LIKE
        );
        long likeCount = updatePromptLikeCountPort.decrease(command.promptId());
        return new LikeInfo(command.promptId(), false, likeCount);
    }
}
