package com.schemaforge.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaForgePropertiesTest {

    @Test
    void rejectsMissingOrShortSecret() {
        assertThat(new SchemaForgeProperties.Jwt(null, Duration.ofHours(1)).isSecretStrong()).isFalse();
        assertThat(new SchemaForgeProperties.Jwt("", Duration.ofHours(1)).isSecretStrong()).isFalse();
        assertThat(new SchemaForgeProperties.Jwt("a".repeat(31), Duration.ofHours(1)).isSecretStrong()).isFalse();
    }

    @Test
    void acceptsSecretOfAtLeast32Bytes() {
        assertThat(new SchemaForgeProperties.Jwt("a".repeat(32), Duration.ofHours(1)).isSecretStrong()).isTrue();
    }
}
