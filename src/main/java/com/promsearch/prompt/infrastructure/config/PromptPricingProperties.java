package com.promsearch.prompt.infrastructure.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "prompt.pricing")
public record PromptPricingProperties(
        @Positive Long premiumPricePoint
) {

    private static final long DEFAULT_PREMIUM_PRICE_POINT = 100L;

    public PromptPricingProperties {
        premiumPricePoint = premiumPricePoint == null
                ? DEFAULT_PREMIUM_PRICE_POINT
                : premiumPricePoint;
    }
}
