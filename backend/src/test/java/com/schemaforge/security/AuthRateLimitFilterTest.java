package com.schemaforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class AuthRateLimitFilterTest {

    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    private static MockHttpServletResponse send(AuthRateLimitFilter filter, String method, String uri, String ip)
            throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.setRemoteAddr(ip);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req, res, new MockFilterChain());
        return res;
    }

    @Test
    void returns429AfterLimitWithinWindow() throws Exception {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:10Z"), ZoneOffset.UTC);
        AuthRateLimitFilter filter = new AuthRateLimitFilter(2, MAPPER, clock);

        assertThat(send(filter, "POST", "/api/v1/auth/login", "1.2.3.4").getStatus()).isEqualTo(200);
        assertThat(send(filter, "POST", "/api/v1/auth/login", "1.2.3.4").getStatus()).isEqualTo(200);
        MockHttpServletResponse limited = send(filter, "POST", "/api/v1/auth/register", "1.2.3.4");

        assertThat(limited.getStatus()).isEqualTo(429);
        assertThat(limited.getHeader("Retry-After")).isEqualTo("50");
        assertThat(limited.getContentAsString()).contains("TOO_MANY_REQUESTS");
    }

    @Test
    void tracksClientsSeparately() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, MAPPER, Clock.systemUTC());
        assertThat(send(filter, "POST", "/api/v1/auth/login", "1.1.1.1").getStatus()).isEqualTo(200);
        assertThat(send(filter, "POST", "/api/v1/auth/login", "2.2.2.2").getStatus()).isEqualTo(200);
    }

    @Test
    void ignoresOtherEndpointsAndMethods() throws Exception {
        AuthRateLimitFilter filter = new AuthRateLimitFilter(1, MAPPER, Clock.systemUTC());
        for (int i = 0; i < 5; i++) {
            assertThat(send(filter, "GET", "/api/v1/auth/login", "9.9.9.9").getStatus()).isEqualTo(200);
            assertThat(send(filter, "POST", "/api/v1/migrations/upload", "9.9.9.9").getStatus()).isEqualTo(200);
        }
    }
}
