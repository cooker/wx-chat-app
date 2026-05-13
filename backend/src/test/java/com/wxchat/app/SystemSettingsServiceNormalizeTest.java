package com.wxchat.app;

import com.wxchat.app.service.SystemSettingsService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SystemSettingsServiceNormalizeTest {

    @Test
    void normalizeBlankMeansLocal() {
        assertEquals("", SystemSettingsService.normalizeImageCdnBase(null));
        assertEquals("", SystemSettingsService.normalizeImageCdnBase(""));
        assertEquals("", SystemSettingsService.normalizeImageCdnBase("   "));
    }

    @Test
    void normalizeAddsHttpsAndTrimsSlash() {
        assertEquals("https://img.example.com", SystemSettingsService.normalizeImageCdnBase("img.example.com"));
        assertEquals("https://img.example.com", SystemSettingsService.normalizeImageCdnBase("https://img.example.com/"));
    }

    @Test
    void normalizeRejectsNonHttpSchemes() {
        assertThrows(IllegalArgumentException.class, () -> SystemSettingsService.normalizeImageCdnBase("ftp://cdn.example.com"));
    }
}
