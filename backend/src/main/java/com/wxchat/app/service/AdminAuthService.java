package com.wxchat.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AdminAuthService {
    private static final long TOKEN_TTL_MS = 24L * 60 * 60 * 1000;

    @Value("${app.admin.auth-enabled:true}")
    private boolean authEnabled;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    private final Map<String, Long> tokens = new ConcurrentHashMap<>();

    public boolean isAuthEnabled() {
        return authEnabled;
    }

    public Optional<String> login(String password) {
        if (!authEnabled) {
            return Optional.of("dev-disabled-auth");
        }
        if (password == null || !password.equals(adminPassword)) {
            return Optional.empty();
        }
        String token = UUID.randomUUID().toString();
        tokens.put(token, System.currentTimeMillis() + TOKEN_TTL_MS);
        return Optional.of(token);
    }

    public void logout(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        tokens.remove(token);
    }

    public boolean validateToken(String token) {
        if (!authEnabled) {
            return true;
        }
        if (token == null || token.isBlank()) {
            return false;
        }
        Long exp = tokens.get(token);
        if (exp == null) {
            return false;
        }
        if (System.currentTimeMillis() > exp) {
            tokens.remove(token);
            return false;
        }
        return true;
    }
}
