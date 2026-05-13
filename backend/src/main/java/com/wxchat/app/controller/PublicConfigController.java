package com.wxchat.app.controller;

import com.wxchat.app.common.ApiResponse;
import com.wxchat.app.service.SystemSettingsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/public")
public class PublicConfigController {
    private final SystemSettingsService systemSettingsService;
    private final String publicBaseUrlOverride;

    public PublicConfigController(
            SystemSettingsService systemSettingsService,
            @Value("${app.public-base-url:}") String publicBaseUrlOverride
    ) {
        this.systemSettingsService = systemSettingsService;
        this.publicBaseUrlOverride = publicBaseUrlOverride == null ? "" : publicBaseUrlOverride;
    }

    @GetMapping("/config")
    public ApiResponse<Map<String, String>> getPublicConfig(HttpServletRequest request) {
        String imageCdnBase = systemSettingsService.getImageCdnBase();
        String uploadServerOrigin = resolveUploadServerOrigin(request, publicBaseUrlOverride);
        int feedPageSize = systemSettingsService.getFeedPageSize();
        int hotAlbumSize = systemSettingsService.getHotAlbumSize();
        Map<String, String> data = new LinkedHashMap<>();
        data.put("imageCdnBase", imageCdnBase);
        data.put("uploadServerOrigin", uploadServerOrigin);
        data.put("feedPageSize", String.valueOf(feedPageSize));
        data.put("hotAlbumSize", String.valueOf(hotAlbumSize));
        return ApiResponse.success(data);
    }

    /**
     * 对外暴露的图片源站根地址（不含路径），用于将 /uploads/... 拼成绝对 URL；
     * 与 CDN 并存时，展示仍走 CDN，预加载/原始资源链需直打源站时可使用该字段。
     */
    public static String resolveUploadServerOrigin(HttpServletRequest request, String configuredOverride) {
        if (configuredOverride != null && !configuredOverride.isBlank()) {
            return trimTrailingSlashes(configuredOverride.trim());
        }
        String scheme = Optional.ofNullable(request.getHeader("X-Forwarded-Proto"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(request.getScheme());
        String hostHeader = Optional.ofNullable(request.getHeader("X-Forwarded-Host"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .orElse(Optional.ofNullable(request.getHeader("Host")).map(String::trim).orElse(""));
        if (!hostHeader.isBlank()) {
            String host = hostHeader.split(",")[0].trim();
            return trimTrailingSlashes(scheme + "://" + host);
        }
        String serverName = request.getServerName();
        if (serverName == null || serverName.isBlank()) {
            return "";
        }
        int port = request.getServerPort();
        boolean defaultHttp = "http".equalsIgnoreCase(scheme);
        boolean defaultHttps = "https".equalsIgnoreCase(scheme);
        boolean appendPort =
                (defaultHttp && port != 80) || (defaultHttps && port != 443) || (!defaultHttp && !defaultHttps);
        String host = appendPort ? serverName + ":" + port : serverName;
        return trimTrailingSlashes(scheme + "://" + host);
    }

    private static String trimTrailingSlashes(String s) {
        String t = s;
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        return t;
    }
}
