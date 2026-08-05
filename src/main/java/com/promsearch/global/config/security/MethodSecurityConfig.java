package com.promsearch.global.config.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@Configuration
@EnableMethodSecurity(proxyTargetClass = true)
public class MethodSecurityConfig {
}
