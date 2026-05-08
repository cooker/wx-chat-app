package com.wxchat.app.controller;

import com.wxchat.app.common.ApiResponse;
import com.wxchat.app.service.AdminAuthService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AdminAuthController {
    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/admin-login")
    public ApiResponse<Map<String, Object>> login(@RequestBody Map<String, Object> body) {
        String password = body.get("password") == null ? "" : String.valueOf(body.get("password"));
        return adminAuthService.login(password)
                .map(token -> {
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", token);
                    return ApiResponse.success(data);
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "密码错误"));
    }

    @PostMapping("/admin-logout")
    public ApiResponse<Void> logout(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        adminAuthService.logout(token == null ? "" : token);
        return ApiResponse.success(null);
    }
}
