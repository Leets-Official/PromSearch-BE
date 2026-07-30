package com.promsearch.community.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class LikeOpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("좋아요 등록과 취소 API를 JWT 필수 커뮤니티 API로 문서화한다")
    @Test
    void likeEndpointsAreDocumentedAsAuthenticated() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode createOperation = document.at("/paths/~1api~1v1~1prompts~1{promptId}~1likes/post");
        JsonNode deleteOperation = document.at("/paths/~1api~1v1~1prompts~1{promptId}~1likes/delete");

        assertThat(createOperation.isMissingNode()).isFalse();
        assertThat(deleteOperation.isMissingNode()).isFalse();
        assertThat(createOperation.path("summary").asText()).contains("[COMMUNITY-001]");
        assertThat(deleteOperation.path("summary").asText()).contains("[COMMUNITY-002]");
        assertThat(createOperation.path("description").asText())
                .contains("작업자: 한하람", "구현 상태: 구현완료");
        assertThat(deleteOperation.path("description").asText())
                .contains("작업자: 한하람", "구현 상태: 구현완료");
        assertThat(createOperation.path("security").get(0).has("jwtBearerAuth")).isTrue();
        assertThat(deleteOperation.path("security").get(0).has("jwtBearerAuth")).isTrue();

        JsonNode communityTag = StreamSupport.stream(document.path("tags").spliterator(), false)
                .filter(tag -> "Community | 커뮤니티".equals(tag.path("name").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(communityTag.path("description").asText()).contains("좋아요", "북마크");
    }

    @DisplayName("JWT 없이 좋아요 API를 호출하면 401을 반환한다")
    @Test
    void likeRequiresJwtAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/{promptId}/likes", 1L))
                .andExpect(status().isUnauthorized());
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
