package com.promsearch.auth.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.auth.infrastructure.external.oauth.GoogleOAuthAdapter;
import com.promsearch.auth.infrastructure.external.oauth.KakaoOAuthAdapter;
import com.promsearch.auth.interfaces.dto.request.SignupRequest;
import com.promsearch.prompt.domain.enums.TagType;
import com.promsearch.prompt.infrastructure.persistence.InterestTagLookupRepository;
import com.promsearch.prompt.infrastructure.persistence.entity.TagJpaEntity;
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
    private InterestTagLookupRepository tagRepository;

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
        if (tagRepository.findAll().stream().noneMatch(tag -> tag.getTagType() == type && tag.getTagName().equals(name))) {
            tagRepository.save(TagJpaEntity.create(type, name, normalizedName, false));
        }
    }

    @Test
    void signupSavesInterestTags() throws Exception {
        Long studentId = tagRepository.findAll().stream().filter(tag -> tag.getTagName().equals("학생")).findFirst().orElseThrow().getId();
        Long developerId = tagRepository.findAll().stream().filter(tag -> tag.getTagName().equals("개발자")).findFirst().orElseThrow().getId();
        Long pptId = tagRepository.findAll().stream().filter(tag -> tag.getTagName().equals("PPT")).findFirst().orElseThrow().getId();
        Long imageId = tagRepository.findAll().stream().filter(tag -> tag.getTagName().equals("이미지 생성")).findFirst().orElseThrow().getId();
        SignupRequest request = new SignupRequest(
                "개발자1",
                "interest@example.com",
                "password123",
                List.of(studentId, developerId),
                List.of(pptId, imageId)
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        var user = userRepository.findByEmail("interest@example.com").orElseThrow();
        assertThat(user.toDomain().getName()).isNull();
        assertThat(user.toDomain().getProfileImageUrl()).isNull();

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
                List.of(1L, 1L),
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
                List.of(999999L),
                List.of()
        );

        mockMvc.perform(post("/api/v1/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail("invalid-interest@example.com")).isEmpty();
    }
}
