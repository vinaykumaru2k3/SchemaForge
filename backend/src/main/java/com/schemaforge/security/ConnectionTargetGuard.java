package com.schemaforge.security;

import com.schemaforge.config.SchemaForgeProperties;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Decides whether the server may open a database connection to a user-supplied host.
 *
 * <p>SchemaForge is usually self-hosted next to internal databases, so private ranges are
 * allowed by default. What is always blocked: link-local addresses (which include the
 * 169.254.169.254 cloud metadata endpoint), the AWS IPv6 metadata address, wildcard and
 * multicast addresses. Operators can restrict further with {@code ALLOWED_DB_HOSTS}.
 *
 * <p>The host is resolved once here and the caller connects to the returned IP literal,
 * so a DNS answer cannot change between the check and the connection.
 */
@Component
public class ConnectionTargetGuard {

    private static final Set<String> BLOCKED_LITERALS = Set.of("fd00:ec2:0:0:0:0:0:254");

    private final Set<String> allowedHosts;
    private final Resolver resolver;

    public ConnectionTargetGuard(SchemaForgeProperties properties) {
        this(properties.connections().allowedHosts(), InetAddress::getAllByName);
    }

    ConnectionTargetGuard(List<String> allowedHosts, Resolver resolver) {
        this.allowedHosts = allowedHosts.stream()
                .map(h -> h.trim().toLowerCase(Locale.ROOT))
                .filter(h -> !h.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        this.resolver = resolver;
    }

    /**
     * @return an IP literal for {@code host} that is safe to connect to, formatted for use in a
     *         JDBC URL (IPv6 wrapped in brackets)
     * @throws IllegalArgumentException if the host is not allowed or cannot be resolved
     */
    public String resolveAllowedHost(String host) {
        String normalized = host == null ? "" : host.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Host must not be blank.");
        }
        if (!allowedHosts.isEmpty() && !allowedHosts.contains(normalized)) {
            throw new IllegalArgumentException("Connections to host '" + host + "' are not allowed on this server.");
        }

        InetAddress[] addresses;
        try {
            addresses = resolver.resolve(normalized);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException("Could not resolve host '" + host + "'.");
        }
        if (addresses.length == 0) {
            throw new IllegalArgumentException("Could not resolve host '" + host + "'.");
        }
        // Every address must be acceptable, otherwise a multi-record name could smuggle in a blocked one.
        if (Arrays.stream(addresses).anyMatch(ConnectionTargetGuard::isBlocked)) {
            throw new IllegalArgumentException("Connections to host '" + host + "' are not allowed on this server.");
        }

        InetAddress chosen = addresses[0];
        String literal = chosen.getHostAddress();
        int zone = literal.indexOf('%');
        if (zone >= 0) {
            literal = literal.substring(0, zone);
        }
        return chosen instanceof Inet6Address ? "[" + literal + "]" : literal;
    }

    static boolean isBlocked(InetAddress address) {
        return address.isLinkLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress()
                || BLOCKED_LITERALS.contains(address.getHostAddress().toLowerCase(Locale.ROOT));
    }

    @FunctionalInterface
    interface Resolver {
        InetAddress[] resolve(String host) throws UnknownHostException;
    }
}
