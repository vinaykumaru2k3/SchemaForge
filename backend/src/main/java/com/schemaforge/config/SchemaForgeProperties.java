package com.schemaforge.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

/**
 * Typed application settings under the {@code schemaforge.*} prefix.
 * Validated at startup so a missing or weak JWT secret stops the app from booting.
 */
@Validated
@ConfigurationProperties(prefix = "schemaforge")
public record SchemaForgeProperties(
        @Valid @NotNull Jwt jwt,
        @Valid @NotNull @DefaultValue Cors cors,
        @Valid @NotNull @DefaultValue Connections connections,
        @Valid @NotNull @DefaultValue RateLimit rateLimit
) {

    public record Jwt(String secret, @DefaultValue("PT24H") Duration expiration) {
        @AssertTrue(message = "schemaforge.jwt.secret (env JWT_SECRET) must be set and at least 32 bytes long")
        public boolean isSecretStrong() {
            return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
        }
    }

    public record Cors(@DefaultValue("http://localhost:5173") List<String> allowedOrigins) {
    }

    public record Connections(@DefaultValue List<String> allowedHosts) {
    }

    public record RateLimit(@Min(1) @DefaultValue("10") int authPerMinute) {
    }
}
