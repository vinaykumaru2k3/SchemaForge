package com.schemaforge.security;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConnectionTargetGuardTest {

    private static InetAddress ip(String literal) {
        try {
            return InetAddress.getByName(literal);
        } catch (UnknownHostException e) {
            throw new IllegalStateException(e);
        }
    }

    private static ConnectionTargetGuard guard(List<String> allowlist, String... resolvesTo) {
        InetAddress[] addresses = java.util.Arrays.stream(resolvesTo).map(ConnectionTargetGuardTest::ip)
                .toArray(InetAddress[]::new);
        return new ConnectionTargetGuard(allowlist, host -> addresses);
    }

    @Test
    void allowsPrivateAddressAndReturnsIpLiteral() {
        assertThat(guard(List.of(), "10.0.0.5").resolveAllowedHost("oracle.internal")).isEqualTo("10.0.0.5");
    }

    @Test
    void wrapsIpv6InBrackets() {
        assertThat(guard(List.of(), "2001:db8::1").resolveAllowedHost("db6")).isEqualTo("[2001:db8:0:0:0:0:0:1]");
    }

    @Test
    void blocksCloudMetadataAddress() {
        assertThatThrownBy(() -> guard(List.of(), "169.254.169.254").resolveAllowedHost("metadata"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void blocksAwsIpv6Metadata() {
        assertThatThrownBy(() -> guard(List.of(), "fd00:ec2::254").resolveAllowedHost("x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blocksWildcardAddress() {
        assertThatThrownBy(() -> guard(List.of(), "0.0.0.0").resolveAllowedHost("0.0.0.0"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void blocksWhenAnyResolvedAddressIsBlocked() {
        assertThatThrownBy(() -> guard(List.of(), "10.0.0.5", "169.254.169.254").resolveAllowedHost("mixed"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enforcesAllowlistCaseInsensitively() {
        ConnectionTargetGuard g = guard(List.of(" Oracle.Prod "), "10.1.1.1");
        assertThat(g.resolveAllowedHost("ORACLE.PROD")).isEqualTo("10.1.1.1");
        assertThatThrownBy(() -> g.resolveAllowedHost("other-host"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsBlankAndUnresolvableHosts() {
        assertThatThrownBy(() -> guard(List.of(), "10.0.0.1").resolveAllowedHost("  "))
                .isInstanceOf(IllegalArgumentException.class);
        ConnectionTargetGuard failing = new ConnectionTargetGuard(List.of(), host -> {
            throw new UnknownHostException(host);
        });
        assertThatThrownBy(() -> failing.resolveAllowedHost("nope.invalid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Could not resolve");
    }
}
