package com.promsearch.user.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class UserOpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Swagger에 내 프로필 조회 API를 노출한다")
    @Test
    void getMyProfileIsDocumented() throws Exception {
        JsonNode document = openApiDocument();

        JsonNode operation = document.at("/paths/~1api~1v1~1users~1me/get");

        assertThat(operation.isMissingNode()).isFalse();
        assertThat(operation.path("description").asText())
                .contains("작업자: 한하람", "구현 상태: 구현완료");
    }

    @DisplayName("Swagger에 닉네임 중복 확인 API와 필수 query parameter를 노출한다")
    @Test
    void nicknameAvailabilityIsDocumented() throws Exception {
        JsonNode operation = openApiDocument()
                .at("/paths/~1api~1v1~1users~1nicknames~1availability/get");

        assertThat(operation.isMissingNode()).isFalse();
        assertThat(operation.at("/parameters/0/name").asText()).isEqualTo("nickname");
        assertThat(operation.at("/parameters/0/required").asBoolean()).isTrue();
        assertThat(operation.path("description").asText())
                .contains("작업자: 한하람", "구현 상태: 구현완료");
    }

    @DisplayName("프로필 조회 응답 스키마는 기본 정보와 관심 태그를 포함한다")
    @Test
    void userProfileSchemaContainsRequiredFields() throws Exception {
        JsonNode properties = openApiDocument().at("/components/schemas/UserProfileResponse/properties");

        assertThat(properties.has("username")).isTrue();
        assertThat(properties.has("profileImageUrl")).isTrue();
        assertThat(properties.has("email")).isTrue();
        assertThat(properties.has("point")).isTrue();
        assertThat(properties.has("gradeName")).isTrue();
        assertThat(properties.has("interestJobTags")).isTrue();
        assertThat(properties.has("interestTaskTags")).isTrue();
    }

    private JsonNode openApiDocument() throws Exception {
        String response = mockMvc.perform(get("/docs-json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }
}
