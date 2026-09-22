package com.vaultdesk.server.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@Order(1)
public class TokenAuthFilter implements Filter {

    private final TokenStore tokenStore;

    // Paths that don't require a token — keep this list tight and explicit.
    private static final List<String> OPEN_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/validate",
            "/api/employee/auth/login",
            "/api/employee/auth/validate",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/employee/auth/forgot-password",
            "/api/employee/auth/reset-password",
            "/api/forgot-password",
            "/api/reset-password",
            "/api/health",
            "/api/version" ,
            "/api/version/download",
            "/api/system-status"
    );

    public TokenAuthFilter(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        String path = request.getRequestURI();

        if (!path.startsWith("/api/")) {
            chain.doFilter(req, res); // static pages/JS/CSS aren't guarded — only /api/** needs a token
            return;
        }

        if (OPEN_PATHS.contains(path)) {
            chain.doFilter(req, res);
            return;
        }

        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer "))
                ? header.substring(7) : null;

        TokenStore.TokenInfo info = tokenStore.validate(token);
        if (info == null) {
            response.setStatus(401);
            response.setContentType("application/json");
            response.getWriter().write("{\"success\":false,\"message\":\"Not authenticated. Please log in again.\"}");
            return;
        }

        try {
            AuthContext.set(info);
            chain.doFilter(req, res);
        } finally {
            AuthContext.clear();
        }
    }
}