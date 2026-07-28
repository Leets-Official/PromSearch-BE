package com.promsearch.prompt.interfaces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class PromptOpenApiContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @DisplayName("Swagger에 프롬프트 이미지 업로드 완료를 포함한 인터페이스를 노출한다")
    @Test
    void promptEndpointsAreDocumented() throws Exception {
        JsonNode document = openApiDocument();

        assertThat(document.at("/paths/~1api~1v1~1prompt-images~1upload-urls/post").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1prompt-images~1{imageId}~1complete/post").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1prompts~1draft/put").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1prompts~1draft/get").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1prompts~1draft/delete").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1prompts/post").isMissingNode()).isFalse();
        assertThat(document.at("/paths/~1api~1v1~1prompts~1{promptId}/delete").isMissingNode()).isFalse();

        JsonNode promptTag = StreamSupport.stream(document.path("tags").spliterator(), false)
                .filter(tag -> "Prompt | 프롬프트".equals(tag.path("name").asText()))
                .findFirst()
                .orElseThrow();
        assertThat(promptTag.path("description").asText())
                .contains("작업자: 한하람", "상태: 구현 중");
    }

    @DisplayName("생성 요청 스키마는 가격 입력과 MASTER를 노출하지 않고 제목을 20자로 제한한다")
    @Test
    void createPromptSchemaReflectsConfirmedPolicy() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode requestProperties = document.at("/components/schemas/CreatePromptRequest/properties");
        JsonNode contentTypeSchema = requestProperties.path("contentType");
        JsonNode contentTypeValues = resolveSchema(document, contentTypeSchema).path("enum");

        assertThat(requestProperties.has("pricePoint")).isFalse();
        assertThat(requestProperties.path("title").path("maxLength").asInt()).isEqualTo(20);
        assertThat(contentTypeValues).extracting(JsonNode::asText)
                .containsExactly("FREE", "PREMIUM");
    }

    @DisplayName("명령 응답은 처리 상태와 작성자 공개 범위를 분리해 노출한다")
    @Test
    void commandResponseSeparatesStatusAndVisibility() throws Exception {
        JsonNode properties = openApiDocument().at("/components/schemas/PromptCommandResponse/properties");

        assertThat(properties.has("promptId")).isTrue();
        assertThat(properties.has("status")).isTrue();
        assertThat(properties.has("visibility")).isTrue();
        assertThat(properties.has("pricePoint")).isTrue();
        assertThat(properties.has("updatedAt")).isTrue();
    }

    @DisplayName("상세 조회 응답은 추천 여부와 전체 추천 수를 분리한다")
    @Test
    void promptDetailUsesRecommendationNaming() throws Exception {
        JsonNode document = openApiDocument();
        JsonNode interaction = document.at(
                "/components/schemas/PromptViewerInteractionResponse/properties");
        JsonNode statistics = document.at(
                "/components/schemas/PromptStatisticsResponse/properties");

        assertThat(interaction.has("recommended")).isTrue();
        assertThat(interaction.has("liked")).isFalse();
        assertThat(statistics.has("recommendCount")).isTrue();
        assertThat(statistics.has("likeCount")).isFalse();
    }

    @DisplayName("상세 조회 접근 사유는 잠금과 해제 원인을 모두 구분한다")
    @Test
    void promptAccessReasonIncludesUnlockedStates() throws Exception {
        JsonNode values = openApiDocument()
                .at("/components/schemas/PromptAccessResponse/properties/reason/enum");

        assertThat(values).extracting(JsonNode::asText)
                .containsExactly("ANONYMOUS", "PREMIUM", "FREE", "AUTHOR", "UNLOCKED");
    }

    private JsonNode openApiDocument() throws Exception {
        String response = mockMvc.perform(get("/docs-json"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response);
    }

    private JsonNode resolveSchema(JsonNode document, JsonNode schema) {
        String reference = schema.path("$ref").asText();
        if (reference.isBlank()) {
            return schema;
        }
        return document.at(reference.substring(1));
    }
}
