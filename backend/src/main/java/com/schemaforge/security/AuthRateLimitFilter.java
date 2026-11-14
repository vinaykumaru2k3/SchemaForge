package com.schemaforge.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemaforge.config.SchemaForgeProperties;
import com.schemaforge.exception.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixed-window, per-client-IP rate limit for the login and register endpoints,
 * to slow down credential stuffing and account spam. In-memory, so per instance.
 */
@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MILLIS = 60_000L;
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int limitPerWindow;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(SchemaForgeProperties properties, ObjectMapper objectMapper) {
        this(properties.rateLimit().authPerMinute(), objectMapper, Clock.systemUTC());
    }

    AuthRateLimitFilter(int limitPerWindow, ObjectMapper objectMapper, Clock clock) {
        this.limitPerWindow = limitPerWindow;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !"POST".equalsIgnoreCase(request.getMethod())
                || !(uri.endsWith("/api/v1/auth/login") || uri.endsWith("/api/v1/auth/register"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = clock.millis();
        long windowStart = now - (now % WINDOW_MILLIS);

        if (windows.size() > MAX_TRACKED_CLIENTS) {
            windows.values().removeIf(w -> w.start < windowStart);
        }

        Window window = windows.compute(request.getRemoteAddr(), (ip, existing) ->
                existing == null || existing.start != windowStart ? new Window(windowStart) : existing);

        if (window.count.incrementAndGet() > limitPerWindow) {
            long retryAfterSeconds = Math.max(1, (windowStart + WINDOW_MILLIS - now) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getOutputStream(), ApiErrorResponse.of(429, "TOO_MANY_REQUESTS",
                    "Too many attempts. Try again in " + retryAfterSeconds + "s.", request.getRequestURI()));
            return;
        }
        chain.doFilter(request, response);
    }

    private static final class Window {
        final long start;
        final AtomicInteger count = new AtomicInteger();

        Window(long start) {
            this.start = start;
        }
    }
}
