package org.clinica.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtConfig(
        String secret,
        Duration expiration
) {
}
