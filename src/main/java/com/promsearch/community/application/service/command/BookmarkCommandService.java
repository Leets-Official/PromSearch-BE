package com.promsearch.community.application.service.command;

import com.promsearch.community.application.port.out.postinteraction.CreatePostInteractionPort;
import com.promsearch.community.application.port.out.postinteraction.DeletePostInteractionIfPresentPort;
import com.promsearch.community.application.port.out.prompt.EnsurePromptInteractionTargetPort;
import com.promsearch.community.application.usecase.BookmarkPromptUseCase;
import com.promsearch.community.application.usecase.UnbookmarkPromptUseCase;
import com.promsearch.community.application.usecase.dto.BookmarkInfo;
import com.promsearch.community.application.usecase.dto.BookmarkPromptCommand;
import com.promsearch.community.domain.PostInteraction;
import com.promsearch.community.domain.enums.InteractionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BookmarkCommandService implements BookmarkPromptUseCase, UnbookmarkPromptUseCase {

    private final CreatePostInteractionPort createPostInteractionPort;
    private final DeletePostInteractionIfPresentPort deletePostInteractionIfPresentPort;
    private final EnsurePromptInteractionTargetPort ensurePromptInteractionTargetPort;

    @Override
    public BookmarkInfo bookmark(BookmarkPromptCommand command) {
        ensurePromptInteractionTargetPort.ensureActivePublicPrompt(command.promptId());
        PostInteraction created = createPostInteractionPort.create(PostInteraction.create(
                command.userId(),
                command.promptId(),
                InteractionType.BOOKMARK
        ));
        return new BookmarkInfo(true, created.getCreatedAt());
    }

    @Override
    public BookmarkInfo unbookmark(BookmarkPromptCommand command) {
        deletePostInteractionIfPresentPort.deleteIfPresent(
                command.userId(),
                command.promptId(),
                InteractionType.BOOKMARK
        );
        return new BookmarkInfo(false, null);
    }
}
