package com.wxchat.app.config;

import com.wxchat.app.service.AdminAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class AdminAuthFilter extends OncePerRequestFilter {
    private final AdminAuthService adminAuthService;

    public AdminAuthFilter(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!adminAuthService.isAuthEnabled()) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        String method = request.getMethod();

        if (isPublic(path, method)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader("X-Admin-Token");
        if (token == null || token.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = auth.substring(7).trim();
            }
        }

        if (!adminAuthService.validateToken(token)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或会话已过期\",\"data\":null}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private static boolean isPublic(String path, String method) {
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/uploads/albums/")) {
            return true;
        }
        if ("POST".equalsIgnoreCase(method) && "/api/auth/admin-login".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/health".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && "/api/albums".equals(path)) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) && path.startsWith("/api/albums/") && path.matches("^/api/albums/\\d+$")) {
            return true;
        }
        return "POST".equalsIgnoreCase(method) && "/api/analytics/track".equals(path);
    }
}
