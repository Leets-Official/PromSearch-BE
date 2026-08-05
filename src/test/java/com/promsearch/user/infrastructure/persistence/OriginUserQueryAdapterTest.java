package com.promsearch.user.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.promsearch.global.config.JpaConfig;
import com.promsearch.user.application.usecase.dto.OriginUserListInfo;
import com.promsearch.user.application.usecase.dto.OriginUserListQuery;
import com.promsearch.user.domain.User;
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
        OriginUserQueryAdapter.class
})
class OriginUserQueryAdapterTest {

    @Autowired
    private OriginUserQueryAdapter adapter;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @DisplayName("Origin 등급 유저만 조회한다")
    @Test
    void listReturnsOnlyOriginGradeUsers() {
        UserJpaEntity originUser = userRepository.saveAndFlush(
                UserJpaEntity.create("origin@promsearch.com", "password", "originUser", "name", null, null)
        );
        promoteToOrigin(originUser);
        userRepository.saveAndFlush(
                UserJpaEntity.create("node@promsearch.com", "password", "nodeUser", "name", null, null)
        );
        entityManager.clear();

        OriginUserListInfo info = adapter.list(new OriginUserListQuery(0, 20));

        assertThat(info.totalElements()).isEqualTo(1);
        assertThat(info.content()).hasSize(1);
        assertThat(info.content().get(0).userId()).isEqualTo(originUser.getId());
        assertThat(info.content().get(0).username()).isEqualTo("originUser");
    }

    private void promoteToOrigin(UserJpaEntity user) {
        // Node -> Link -> Sync -> Core -> Prime 까지 자동 승급을 4회 적용한다.
        for (int i = 0; i < 4; i++) {
            user.promoteGrade();
        }
        User promoted = user.toDomain().promoteToOrigin();
        user.updateFrom(promoted);
        userRepository.saveAndFlush(user);
    }
}
