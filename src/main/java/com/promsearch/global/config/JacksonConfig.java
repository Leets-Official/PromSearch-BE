package com.promsearch.global.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer(
            @Value("${spring.jackson.time-zone:Asia/Seoul}") String responseTimeZone
    ) {
        ZoneId zoneId = ZoneId.of(responseTimeZone);
        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addSerializer(Instant.class, new ZonedInstantSerializer(zoneId));

        return builder -> builder
                .timeZone(TimeZone.getTimeZone(zoneId))
                .modulesToInstall(javaTimeModule)
                .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    private static final class ZonedInstantSerializer extends JsonSerializer<Instant> {

        private final ZoneId zoneId;

        private ZonedInstantSerializer(ZoneId zoneId) {
            this.zoneId = zoneId;
        }

        @Override
        public void serialize(
                Instant value,
                JsonGenerator generator,
                SerializerProvider serializers
        ) throws IOException {
            generator.writeString(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value.atZone(zoneId)));
        }
    }
}
