package com.promsearch.global.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.Ordered;

class RequestLoggingConfigTest {

    @DisplayName("요청 로깅 필터를 Security보다 앞선 최우선 순서로 등록한다")
    @Test
    void registerRequestLoggingFilterFirst() {
        RequestLoggingConfig config = new RequestLoggingConfig();
        RequestLoggingFilter filter = config.requestLoggingFilter();

        FilterRegistrationBean<RequestLoggingFilter> registration =
                config.requestLoggingFilterRegistration(filter);

        assertThat(registration.getFilter()).isSameAs(filter);
        assertThat(registration.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
