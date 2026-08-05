package com.promsearch.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.user.domain.enums.GradeRequestStatus;
import com.promsearch.user.domain.enums.UserGrade;
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
        UserGradePromotionPersistenceAdapter.class
})
class UserGradePromotionPersistenceAdapterTest {

    @Autowired
    private UserGradePromotionPersistenceAdapter adapter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GradeRequestRepository gradeRequestRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("게시글 작성 시 다음 등급으로 1단계 승급한다")
    @Test
    void promoteOneStep() {
        Long userId = saveUser("user1@promsearch.com", "nickname1");

        adapter.promoteForPostCreation(userId);
        entityManager.clear();

        assertThat(userRepository.findById(userId).orElseThrow().toDomain().getGrade())
                .isEqualTo(UserGrade.LINK);
    }

    @DisplayName("Prime에 처음 도달하면 Origin 심사 대기 항목을 자동 생성한다")
    @Test
    void createsGradeRequestWhenReachingPrimeForTheFirstTime() {
        Long userId = saveUser("user2@promsearch.com", "nickname2");

        // Node -> Link -> Sync -> Core -> Prime: 네 번째 게시글 작성에서 Prime에 도달한다
        adapter.promoteForPostCreation(userId);
        adapter.promoteForPostCreation(userId);
        adapter.promoteForPostCreation(userId);
        adapter.promoteForPostCreation(userId);
        entityManager.clear();

        assertThat(userRepository.findById(userId).orElseThrow().toDomain().getGrade())
                .isEqualTo(UserGrade.PRIME);
        assertThat(gradeRequestRepository.existsByUserIdAndStatus(userId, GradeRequestStatus.PENDING))
                .isTrue();
        assertThat(gradeRequestRepository.count()).isEqualTo(1);
    }

    @DisplayName("Prime 유저가 게시글을 더 작성해도 등급과 심사 대기 항목은 변하지 않는다")
    @Test
    void doesNotChangeOrDuplicateOncePrime() {
        Long userId = saveUser("user3@promsearch.com", "nickname3");
        for (int i = 0; i < 4; i++) {
            adapter.promoteForPostCreation(userId);
        }
        entityManager.clear();

        adapter.promoteForPostCreation(userId);
        entityManager.clear();

        assertThat(userRepository.findById(userId).orElseThrow().toDomain().getGrade())
                .isEqualTo(UserGrade.PRIME);
        assertThat(gradeRequestRepository.count()).isEqualTo(1);
    }

    private Long saveUser(String email, String nickname) {
        UserJpaEntity user = userRepository.saveAndFlush(
                UserJpaEntity.create(email, "password", nickname, "name", null)
        );
        return user.toDomain().getUserId().id();
    }
}
