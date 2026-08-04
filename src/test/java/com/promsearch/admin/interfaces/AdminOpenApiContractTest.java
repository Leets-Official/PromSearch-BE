package com.promsearch.admin.interfaces;

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
class AdminOpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Swagger에 신고함·등급업 관리자 API 4개를 노출한다")
    @Test
    void adminEndpointsAreDocumented() throws Exception {
        JsonNode document = openApiDocument();

        assertThat(document.at("/paths/~1api~1v1~1admin~1reports/get").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1admin~1reports~1{reportId}/patch").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1admin~1grade-requests/get").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1admin~1grade-requests~1{requestId}/patch").isMissingNode()).isFalse();

        assertThat(document.at("/paths/~1api~1v1~1admin~1grade-requests/get/description").asText())
                .contains("작업자: kallin1", "구현 상태: 미구현");
        assertThat(document.at("/paths/~1api~1v1~1admin~1grade-requests~1{requestId}/patch/description").asText())
                .contains("작업자: kallin1", "구현 상태: 미구현");
    }

    @DisplayName("신고함 관리자 API는 구현 완료 상태로 문서화된다")
    @Test
    void reportEndpointsAreDocumentedAsImplemented() throws Exception {
        JsonNode document = openApiDocument();

        assertThat(document.at("/paths/~1api~1v1~1admin~1reports/get/description").asText())
                .doesNotContain("미구현");
        assertThat(document.at("/paths/~1api~1v1~1admin~1reports~1{reportId}/patch/description").asText())
                .doesNotContain("미구현");
    }

    @DisplayName("신고 생성 API 미구현 상태를 Swagger 설명에 명시한다")
    @Test
    void reportTagDocumentsUnimplementedCreation() throws Exception {
        JsonNode document = openApiDocument();

        String reportTagDescription = tagDescription(document, "Admin | 신고함");
        assertThat(reportTagDescription).contains("신고 생성 API는 이번 범위에서 미구현");
    }

    @DisplayName("등급업 신청 생성 방식 미정 상태를 Swagger 설명에 명시한다")
    @Test
    void gradeRequestTagDocumentsUndecidedPolicy() throws Exception {
        JsonNode document = openApiDocument();

        String gradeTagDescription = tagDescription(document, "Admin | Origin 등급업");
        assertThat(gradeTagDescription).contains("정책 미정");
    }

    private String tagDescription(JsonNode document, String tagName) {
        return java.util.stream.StreamSupport.stream(document.path("tags").spliterator(), false)
                .filter(tag -> tagName.equals(tag.path("name").asText()))
                .findFirst()
                .orElseThrow()
                .path("description")
                .asText();
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
