package com.promsearch.community.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class BookmarkOpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("북마크 등록·취소·내 목록 API를 JWT 필수 API로 문서화한다")
    @Test
    void bookmarkEndpointsAreDocumentedAsAuthenticated() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode create = document.at("/paths/~1api~1v1~1prompts~1{promptId}~1bookmarks/post");
        JsonNode delete = document.at("/paths/~1api~1v1~1prompts~1{promptId}~1bookmarks/delete");
        JsonNode list = document.at("/paths/~1api~1v1~1users~1me~1bookmarks/get");

        assertThat(create.path("summary").asText()).contains("[COMMUNITY-003]");
        assertThat(delete.path("summary").asText()).contains("[COMMUNITY-004]");
        assertThat(list.path("summary").asText()).contains("[COMMUNITY-005]");
        assertThat(create.path("security").get(0).has("jwtBearerAuth")).isTrue();
        assertThat(delete.path("security").get(0).has("jwtBearerAuth")).isTrue();
        assertThat(list.path("security").get(0).has("jwtBearerAuth")).isTrue();
        assertThat(list.path("parameters").toString())
                .contains("taskTagId", "aiModelTagId", "outputType", "page", "size");
    }

    @DisplayName("JWT 없이 북마크 등록과 내 북마크 목록을 호출하면 401을 반환한다")
    @Test
    void bookmarkRequiresJwtAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/prompts/{promptId}/bookmarks", 1L))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me/bookmarks"))
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
