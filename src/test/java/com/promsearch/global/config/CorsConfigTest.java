package com.promsearch.global.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorsConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @DisplayName("허용된 배포 도메인은 인증이 필요한 API에도 preflight 요청이 통과한다")
    @Test
    void allowedOriginPassesPreflightOnProtectedApi() throws Exception {
        mockMvc.perform(options("/api/v1/prompts/draft")
                        .header(HttpHeaders.ORIGIN, "https://promsearch.kr")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://promsearch.kr"));
    }

    @DisplayName("Vercel 프리뷰 배포 와일드카드 도메인도 preflight 요청이 통과한다")
    @Test
    void vercelPreviewWildcardOriginPassesPreflight() throws Exception {
        mockMvc.perform(options("/api/v1/prompts/draft")
                        .header(HttpHeaders.ORIGIN, "https://pr-123.prom-search-fe.vercel.app")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://pr-123.prom-search-fe.vercel.app"));
    }

    @DisplayName("허용 목록에 없는 도메인은 CORS 허용 헤더 없이 거부된다")
    @Test
    void disallowedOriginIsRejected() throws Exception {
        mockMvc.perform(options("/api/v1/prompts/draft")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @DisplayName("허용된 도메인에서의 실제 요청은 Authorization 헤더를 노출한다")
    @Test
    void allowedOriginExposesAuthorizationHeader() throws Exception {
        mockMvc.perform(get("/test/health-check")
                        .header(HttpHeaders.ORIGIN, "https://promsearch.kr"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://promsearch.kr"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Authorization"));
    }
}
