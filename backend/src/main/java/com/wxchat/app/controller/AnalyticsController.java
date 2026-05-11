package com.wxchat.app.controller;

import com.wxchat.app.common.ApiResponse;
import com.wxchat.app.service.AnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
public class AnalyticsController {
    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(analyticsService.getOverview());
    }

    @PostMapping("/track")
    public ApiResponse<Void> track(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        long albumId = Long.parseLong(String.valueOf(body.getOrDefault("albumId", "0")));
        String visitorId = String.valueOf(body.getOrDefault("visitorId", "anonymous"));
        String ip = extractClientIp(request);
        String ua = extractUserAgent(request);
        analyticsService.trackAccess(albumId, visitorId, ip, ua);
        return ApiResponse.success(null);
    }

    @GetMapping("/events")
    public ApiResponse<Map<String, Object>> events(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return ApiResponse.success(analyticsService.listEvents(page, pageSize));
    }

    /** 今日去重后的访问 IP（供管理端 IP 画像批量展示）。 */
    @GetMapping("/today-ips")
    public ApiResponse<Map<String, Object>> todayDistinctIps() {
        return ApiResponse.success(Map.of("ips", analyticsService.listTodayDistinctIps()));
    }

    @DeleteMapping("/events")
    public ApiResponse<Void> clearEvents() {
        analyticsService.clearEvents();
        return ApiResponse.success(null);
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }

    private String extractUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null || userAgent.isBlank()) {
            return "unknown";
        }
        return userAgent;
    }
}
