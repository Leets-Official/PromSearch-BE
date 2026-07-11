package com.promsearch.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promsearch.global.response.ApiResponse;
import com.promsearch.global.response.code.BaseCode;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;

public final class ApiSecurityResponseWriter {

    private ApiSecurityResponseWriter() {
    }

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, BaseCode errorCode)
            throws IOException {
        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage(), null)
        );
    }
}
