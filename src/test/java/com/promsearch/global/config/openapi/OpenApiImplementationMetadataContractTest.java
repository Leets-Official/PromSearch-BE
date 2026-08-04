package com.promsearch.global.config.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "springdoc.api-docs.enabled=true")
@AutoConfigureMockMvc
class OpenApiImplementationMetadataContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Swagger의 모든 명세 API에 작업자와 실제 구현 상태를 표시한다")
    @Test
    void allDocumentedOperationsExposeWorkerAndImplementationStatus() throws Exception {
        JsonNode paths = openApiDocument().path("paths");
        List<String> documentedOperations = new ArrayList<>();
        List<String> missingMetadata = new ArrayList<>();
        List<String> invalidStatus = new ArrayList<>();

        paths.properties().forEach(pathEntry ->
                pathEntry.getValue().properties().forEach(methodEntry -> {
                    JsonNode operation = methodEntry.getValue();
                    String summary = operation.path("summary").asText();
                    if (!summary.startsWith("[")) {
                        return;
                    }

                    String operationKey = methodEntry.getKey().toUpperCase() + " " + pathEntry.getKey();
                    String description = operation.path("description").asText();
                    documentedOperations.add(operationKey);
                    if (!description.contains("작업자:") || !description.contains("구현 상태:")) {
                        missingMetadata.add(operationKey);
                    }
                    if (!description.contains("구현 상태: 구현완료")
                            && !description.contains("구현 상태: 미구현")) {
                        invalidStatus.add(operationKey);
                    }
                }));

        assertThat(documentedOperations).hasSize(42);
        assertThat(missingMetadata).isEmpty();
        assertThat(invalidStatus).isEmpty();
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
