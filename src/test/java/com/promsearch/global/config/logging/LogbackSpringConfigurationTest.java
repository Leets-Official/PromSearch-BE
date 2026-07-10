package com.promsearch.global.config.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class LogbackSpringConfigurationTest {

    @DisplayName("logback-spring.xml은 local 텍스트 로그와 dev/prod JSON encoder 설정을 포함한다")
    @Test
    void logbackSpringXmlDefinesProfileAwareAppenders() throws Exception {
        ClassPathResource resource = new ClassPathResource("logback-spring.xml");

        assertThat(resource.exists()).isTrue();
        String xml = resource.getContentAsString(StandardCharsets.UTF_8);

        assertThat(xml).contains("<springProfile name=\"local,default\">")
                .contains("PatternLayoutEncoder")
                .contains("<springProfile name=\"dev,prod\">")
                .contains("net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder")
                .contains("<timestamp>")
                .contains("<logLevel>")
                .contains("<loggerName>")
                .contains("<message>")
                .contains("<mdc>")
                .contains("<includeMdcKeyName>requestId</includeMdcKeyName>")
                .contains("<includeMdcKeyName>traceId</includeMdcKeyName>")
                .contains("<includeMdcKeyName>method</includeMdcKeyName>")
                .contains("<includeMdcKeyName>path</includeMdcKeyName>")
                .contains("<includeMdcKeyName>clientIp</includeMdcKeyName>")
                .contains("<includeMdcKeyName>userId</includeMdcKeyName>")
                .contains("<includeMdcKeyName>memberId</includeMdcKeyName>")
                .contains("<includeMdcKeyName>event</includeMdcKeyName>")
                .contains("<includeMdcKeyName>uriTemplate</includeMdcKeyName>")
                .contains("<includeMdcKeyName>statusCode</includeMdcKeyName>")
                .contains("<includeMdcKeyName>durationMs</includeMdcKeyName>")
                .contains("<includeMdcKeyName>exception</includeMdcKeyName>")
                .contains("<stackTrace>")
                .doesNotContain("<headers")
                .doesNotContain("<arguments")
                .doesNotContain("Authorization")
                .doesNotContain("requestBody")
                .doesNotContain("password");
        assertThat(Class.forName("net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder")).isNotNull();
    }
}
