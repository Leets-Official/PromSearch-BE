package com.promsearch.prompt.application.service.query;

import com.promsearch.prompt.application.port.out.prompt.HomePromptReader;
import com.promsearch.prompt.application.port.out.prompt.LoadPromptPort;
import com.promsearch.prompt.application.port.out.prompt.PromptPageResult;
import com.promsearch.prompt.application.usecase.GetMyPromptInsightsUseCase;
import com.promsearch.prompt.application.usecase.ListHomePromptsUseCase;
import com.promsearch.prompt.application.usecase.ListMyPromptsUseCase;
import com.promsearch.prompt.application.usecase.dto.HomePromptListInfo;
import com.promsearch.prompt.application.usecase.dto.HomePromptListQuery;
import com.promsearch.prompt.application.usecase.dto.ListMyPromptsQuery;
import com.promsearch.prompt.application.usecase.dto.MyPromptPageInfo;
import com.promsearch.prompt.application.usecase.dto.MyPromptSummaryInfo;
import com.promsearch.prompt.application.usecase.dto.PromptInsightInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PromptQueryService implements ListHomePromptsUseCase, ListMyPromptsUseCase, GetMyPromptInsightsUseCase {

    /*
     * application 계층은 JPA Repository나 EntityManager를 직접 알지 않습니다.
     * HomePromptReader 포트를 통해 "홈 카드 목록을 읽는다"는 의도만 표현하고,
     * 실제 JPQL/영속성 세부사항은 infrastructure adapter가 담당합니다.
     */
    private final HomePromptReader homePromptReader;
    private final LoadPromptPort loadPromptPort;

    @Override
    public HomePromptListInfo listPrompts(HomePromptListQuery query) {
        /*
         * 홈 필터 목록은 직군, 태스크, AI 모델, 결과물 타입, 검색어가 한 번에 조합됩니다.
         * application 계층은 조건 객체만 전달하고, 실제 JPQL 조합은 infrastructure 어댑터가 담당합니다.
         */
        return homePromptReader.listPrompts(query);
    }

    @Override
    public HomePromptListInfo listPopularPrompts(HomePromptListQuery query) {
        /*
         * 5주차 회의록 기준으로 인기 목록은 북마크가 아니라 좋아요 기반 정렬입니다.
         * 실제 정렬 조건은 조회 성능과 페이지네이션 안정성을 위해 persistence adapter에서 고정합니다.
         */
        return homePromptReader.listPopularPrompts(query);
    }

    @Override
    public HomePromptListInfo listJobPrompts(HomePromptListQuery query) {
        /*
         * 직군별 목록도 같은 홈 카드 응답을 사용하지만 정렬/필터 기준은 다릅니다.
         * 이 차이는 persistence adapter에서 query.jobTagId() 조건으로 분기합니다.
         */
        return homePromptReader.listJobPrompts(query);
    }

    @Override
    public MyPromptPageInfo listMyPrompts(ListMyPromptsQuery query) {
        PromptPageResult result = loadPromptPort.listByUserIdAndStatus(
                query.userId(),
                query.status(),
                query.visibility(),
                query.page(),
                query.size()
        );

        return new MyPromptPageInfo(
                result.content().stream().map(MyPromptSummaryInfo::from).toList(),
                result.totalElements()
        );
    }

    @Override
    public PromptInsightInfo getMyPromptInsights(Long userId) {
        return PromptInsightInfo.from(loadPromptPort.sumInsightsByUserId(userId));
    }
}
