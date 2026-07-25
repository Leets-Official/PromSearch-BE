package com.promsearch.prompt.application;

import com.promsearch.prompt.application.port.out.HomePromptReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptQueryService implements ListHomePromptsUseCase {

    private final HomePromptReader homePromptReader;

    @Override
    public HomePromptListInfo listPopularPrompts(HomePromptListQuery query) {
        /*
         * Product rule from the week-5 notes:
         * "popular" means like-based ranking, not bookmark-based ranking.
         * The exact ordering is fixed in the persistence adapter.
         */
        return homePromptReader.listPopularPrompts(query);
    }

    @Override
    public HomePromptListInfo listJobPrompts(HomePromptListQuery query) {
        return homePromptReader.listJobPrompts(query);
    }
}
