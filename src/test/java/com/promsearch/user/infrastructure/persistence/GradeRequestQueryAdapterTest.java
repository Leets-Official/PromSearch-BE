package com.promsearch.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.prompt.domain.Prompt;
import com.promsearch.prompt.domain.enums.PromptContentType;
import com.promsearch.prompt.domain.enums.PromptOutputType;
import com.promsearch.prompt.domain.enums.PromptVisibility;
import com.promsearch.prompt.infrastructure.persistence.PostRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.PostJpaEntity;
import com.promsearch.prompt.infrastructure.persistence.entity.PostStatisticsJpaEntity;
import com.promsearch.user.application.usecase.dto.GradeRequestListInfo;
import com.promsearch.user.application.usecase.dto.GradeRequestListQuery;
import com.promsearch.user.application.usecase.dto.GradeRequestSummaryInfo;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
import com.promsearch.user.domain.exception.UserDomainException;
import com.promsearch.user.domain.exception.UserErrorCode;
import com.promsearch.user.infrastructure.persistence.entity.GradeRequestJpaEntity;
import com.promsearch.user.infrastructure.persistence.entity.UserJpaEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({
        JpaConfig.class,
        GradeRequestQueryAdapter.class
})
class GradeRequestQueryAdapterTest {

    @Autowired
    private GradeRequestQueryAdapter adapter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradeRequestRepository gradeRequestRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("상태로 필터링한 심사 대기 목록은 게시글 수와 누적 추천 수를 함께 반환한다")
    @Test
    void listReturnsSummaryWithPostAndLikeStats() {
        UserJpaEntity user = userRepository.saveAndFlush(
                UserJpaEntity.create("user@promsearch.com", "password", "hanharam", "name", null, null)
        );
        savePost(user.getId(), 3);
        savePost(user.getId(), 5);
        GradeRequestJpaEntity gradeRequest = gradeRequestRepository.saveAndFlush(
                GradeRequestJpaEntity.createPendingOriginRequest(user.getId())
        );
        entityManager.clear();

        GradeRequestListInfo pending = adapter.list(new GradeRequestListQuery(GradeRequestStatus.PENDING, 0, 20));

        assertThat(pending.totalElements()).isEqualTo(1);
        assertThat(pending.hasNext()).isFalse();
        GradeRequestSummaryInfo summary = pending.content().get(0);
        assertThat(summary.gradeRequestId()).isEqualTo(gradeRequest.getId());
        assertThat(summary.userId()).isEqualTo(user.getId());
        assertThat(summary.username()).isEqualTo("user@promsearch.com");
        assertThat(summary.nickname()).isEqualTo("hanharam");
        assertThat(summary.currentGrade()).isEqualTo(UserGrade.PRIME);
        assertThat(summary.requestedGrade()).isEqualTo(UserGrade.ORIGIN);
        assertThat(summary.postCount()).isEqualTo(2);
        assertThat(summary.totalLikeCount()).isEqualTo(8);

        GradeRequestListInfo rejected = adapter.list(new GradeRequestListQuery(GradeRequestStatus.REJECTED, 0, 20));
        assertThat(rejected.content()).isEmpty();
        assertThat(rejected.totalElements()).isZero();
    }

    @DisplayName("식별자로 단건 조회하면 동일한 통계를 반환한다")
    @Test
    void getByIdReturnsSummary() {
        UserJpaEntity user = userRepository.saveAndFlush(
                UserJpaEntity.create("user2@promsearch.com", "password", "nickname2", "name", null, null)
        );
        GradeRequestJpaEntity gradeRequest = gradeRequestRepository.saveAndFlush(
                GradeRequestJpaEntity.createPendingOriginRequest(user.getId())
        );
        entityManager.clear();

        GradeRequestSummaryInfo summary = adapter.getById(gradeRequest.getId());

        assertThat(summary.userId()).isEqualTo(user.getId());
        assertThat(summary.postCount()).isZero();
        assertThat(summary.totalLikeCount()).isZero();
    }

    @DisplayName("존재하지 않는 식별자를 조회하면 예외가 발생한다")
    @Test
    void getByIdThrowsWhenMissing() {
        assertThatThrownBy(() -> adapter.getById(9_999L))
                .isInstanceOf(UserDomainException.class)
                .extracting("baseCode")
                .isEqualTo(UserErrorCode.GRADE_REQUEST_NOT_FOUND);
    }

    private void savePost(Long userId, int likeCount) {
        Prompt prompt = Prompt.createActive(
                userId, "제목", "본문", PromptOutputType.TEXT, "설명",
                PromptContentType.FREE, PromptVisibility.PUBLIC, 0L
        );
        PostJpaEntity post = PostJpaEntity.from(prompt);
        PostStatisticsJpaEntity statistics = PostStatisticsJpaEntity.create(post);
        for (int i = 0; i < likeCount; i++) {
            statistics.increaseLikeCount();
        }
        post.initializeStatistics(statistics);
        postRepository.saveAndFlush(post);
    }
}
