package com.promsearch.auth.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.infrastructure.external.oauth.GoogleOAuthAdapter;
import com.promsearch.auth.infrastructure.external.oauth.KakaoOAuthAdapter;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
import com.promsearch.user.infrastructure.persistence.InterestTagCatalogRepository;
import com.promsearch.user.infrastructure.persistence.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SignupProfileInterestIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterestTagCatalogRepository tagRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private KakaoOAuthAdapter kakaoOAuthAdapter;

    @MockitoBean
    private GoogleOAuthAdapter googleOAuthAdapter;

    @BeforeEach
    void setUpInterestTags() {
        saveTagIfMissing(TagType.JOB, "학생", "학생");
        saveTagIfMissing(TagType.JOB, "개발자", "개발자");
        saveTagIfMissing(TagType.TASK, "PPT", "ppt");
        saveTagIfMissing(TagType.TASK, "이미지 생성", "이미지 생성");
    }

    private void saveTagIfMissing(TagType type, String name, String normalizedName) {
        if (tagRepository.findAllByTagTypeAndTagNameIn(type, List.of(name)).isEmpty()) {
            tagRepository.save(TagJpaEntity.create(type, name, normalizedName, false));
        }
    }

    @Test
    void signupSavesOptionalProfileImageAndInterestTags() throws Exception {
        SignupRequest request = new SignupRequest(
                "개발자1",
                "interest@example.com",
                "password123",
                "https://cdn.promsearch.com/profiles/me.png",
                List.of("학생", "개발자"),
                List.of("PPT", "이미지 생성")
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("interest@example.com").orElseThrow();
        assertThat(user.toDomain().getName()).isNull();
        assertThat(user.toDomain().getProfileImageUrl())
                .isEqualTo("https://cdn.promsearch.com/profiles/me.png");

        Integer tagCount = jdbcTemplate.queryForObject(
                "select count(*) from user_interest_tags where user_id = ?",
                Integer.class,
                user.toDomain().getUserId().id()
        );
        assertThat(tagCount).isEqualTo(4);
    }

    @Test
    void signupRejectsDuplicateInterestTags() throws Exception {
        SignupRequest request = new SignupRequest(
                "개발자2",
                "duplicate-interest@example.com",
                "password123",
                null,
                List.of("개발자", "개발자"),
                List.of()
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupRollsBackUserWhenInterestTagDoesNotExist() throws Exception {
        SignupRequest request = new SignupRequest(
                "개발자3",
                "invalid-interest@example.com",
                "password123",
                null,
                List.of("존재하지 않는 직군"),
                List.of()
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail("invalid-interest@example.com")).isEmpty();
    }
}
