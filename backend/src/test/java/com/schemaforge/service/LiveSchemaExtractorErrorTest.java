package com.schemaforge.service;

import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

class LiveSchemaExtractorErrorTest {

    @Test
    void mapsKnownOracleCodesToFriendlyText() {
        assertThat(LiveSchemaExtractor.describe(new SQLException("ORA-01017: invalid username/password", "72000", 1017)))
                .isEqualTo("Invalid username or password.");
        assertThat(LiveSchemaExtractor.describe(new SQLException("IO Error", "08006", 17002)))
                .contains("Could not reach");
    }

    @Test
    void neverEchoesConnectionDescriptors() {
        String raw = "ORA-12345: weird (DESCRIPTION=(ADDRESS=(HOST=10.0.0.9)(PORT=1521))(CONNECTION_ID=abc==))";
        String described = LiveSchemaExtractor.describe(new SQLException(raw, "99999", 12345));
        assertThat(described).contains("ORA-12345").doesNotContain("10.0.0.9").doesNotContain("CONNECTION_ID");
    }

    @Test
    void fallsBackToGenericMessage() {
        assertThat(LiveSchemaExtractor.describe(new SQLException("something at jdbc:oracle:thin:@//h:1/s")))
                .doesNotContain("jdbc:");
    }
}
