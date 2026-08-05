package com.promsearch.prompt.infrastructure.config;

import com.promsearch.prompt.application.port.out.pricing.LoadPromptPricingPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ConfiguredPromptPricingAdapter implements LoadPromptPricingPort {

    private final PromptPricingProperties properties;

    @Override
    public long getPremiumPricePoint() {
        return properties.premiumPricePoint();
    }
}
