package com.wxchat.app.controller;

import com.wxchat.app.common.ApiResponse;
import com.wxchat.app.service.SystemSettingsService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/settings")
public class AdminSettingsController {
    private final SystemSettingsService systemSettingsService;

    public AdminSettingsController(SystemSettingsService systemSettingsService) {
        this.systemSettingsService = systemSettingsService;
    }

    @GetMapping("/image-cdn")
    public ApiResponse<Map<String, String>> getImageCdn() {
        return ApiResponse.success(Map.of("imageCdnBase", systemSettingsService.getImageCdnBase()));
    }

    @PutMapping("/image-cdn")
    public ApiResponse<Map<String, String>> putImageCdn(@RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("imageCdnBase");
        String input = raw == null ? "" : String.valueOf(raw);
        try {
            String stored = systemSettingsService.setImageCdnBase(input);
            return ApiResponse.success(Map.of("imageCdnBase", stored));
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/feed-page")
    public ApiResponse<Map<String, Integer>> getFeedPage() {
        return ApiResponse.success(Map.of("feedPageSize", systemSettingsService.getFeedPageSize()));
    }

    @PutMapping("/feed-page")
    public ApiResponse<Map<String, Integer>> putFeedPage(@RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("feedPageSize");
        int stored = systemSettingsService.setFeedPageSize(raw);
        return ApiResponse.success(Map.of("feedPageSize", stored));
    }

    @GetMapping("/hot-album-size")
    public ApiResponse<Map<String, Integer>> getHotAlbumSize() {
        return ApiResponse.success(Map.of("hotAlbumSize", systemSettingsService.getHotAlbumSize()));
    }

    @PutMapping("/hot-album-size")
    public ApiResponse<Map<String, Integer>> putHotAlbumSize(@RequestBody Map<String, Object> body) {
        Object raw = body == null ? null : body.get("hotAlbumSize");
        int stored = systemSettingsService.setHotAlbumSize(raw);
        return ApiResponse.success(Map.of("hotAlbumSize", stored));
    }

    @GetMapping("/site-header")
    public ApiResponse<Map<String, String>> getSiteHeader() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("topTitle", systemSettingsService.getSiteTopTitle());
        data.put("topDescription", systemSettingsService.getSiteTopDescription());
        data.put("headerScript", systemSettingsService.getSiteHeaderScript());
        return ApiResponse.success(data);
    }

    @PutMapping("/site-header")
    public ApiResponse<Map<String, String>> putSiteHeader(@RequestBody Map<String, Object> body) {
        String topTitle = stringField(body, "topTitle");
        String topDescription = stringField(body, "topDescription");
        String headerScript = stringField(body, "headerScript");
        String storedTitle = systemSettingsService.setSiteTopTitle(topTitle);
        String storedDesc = systemSettingsService.setSiteTopDescription(topDescription);
        String storedScript = systemSettingsService.setSiteHeaderScript(headerScript);
        Map<String, String> data = new LinkedHashMap<>();
        data.put("topTitle", storedTitle);
        data.put("topDescription", storedDesc);
        data.put("headerScript", storedScript);
        return ApiResponse.success(data);
    }

    private static String stringField(Map<String, Object> body, String key) {
        if (body == null) {
            return "";
        }
        Object v = body.get(key);
        return v == null ? "" : String.valueOf(v);
    }
}
