package com.wxchat.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;

@Service
public class SystemSettingsService {
    static final String KEY_IMAGE_CDN_BASE = "image_cdn_base";
    static final String KEY_FEED_PAGE_SIZE = "feed_page_size";
    static final int FEED_PAGE_SIZE_DEFAULT = 8;
    static final int FEED_PAGE_SIZE_MIN = 1;
    static final int FEED_PAGE_SIZE_MAX = 100;

    private final JdbcTemplate jdbcTemplate;

    public SystemSettingsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureTable();
    }

    private void ensureTable() {
        jdbcTemplate.execute(
                """
                        CREATE TABLE IF NOT EXISTS system_settings (
                            setting_key TEXT PRIMARY KEY,
                            setting_value TEXT NOT NULL DEFAULT ''
                        )
                        """
        );
        jdbcTemplate.update(
                "INSERT OR IGNORE INTO system_settings (setting_key, setting_value) VALUES (?, '')",
                KEY_IMAGE_CDN_BASE
        );
        jdbcTemplate.update(
                "INSERT OR IGNORE INTO system_settings (setting_key, setting_value) VALUES (?, ?)",
                KEY_FEED_PAGE_SIZE,
                String.valueOf(FEED_PAGE_SIZE_DEFAULT)
        );
    }

    public String getImageCdnBase() {
        String value = jdbcTemplate.query(
                "SELECT setting_value FROM system_settings WHERE setting_key = ?",
                rs -> rs.next() ? rs.getString(1) : "",
                KEY_IMAGE_CDN_BASE
        );
        return value == null ? "" : value;
    }

    /**
     * @param raw user input; blank means same-origin (no CDN)
     * @return normalized stored value
     * @throws IllegalArgumentException if non-blank but not a valid http(s) origin
     */
    @Transactional
    public String setImageCdnBase(String raw) {
        String normalized = normalizeImageCdnBase(raw);
        jdbcTemplate.update(
                """
                        INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?)
                        ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
                        """,
                KEY_IMAGE_CDN_BASE,
                normalized
        );
        return normalized;
    }

    public static String normalizeImageCdnBase(String raw) {
        if (raw == null) {
            return "";
        }
        String t = raw.trim();
        if (t.isEmpty()) {
            return "";
        }
        int schemeSep = t.indexOf("://");
        if (schemeSep > 0) {
            String scheme = t.substring(0, schemeSep).toLowerCase();
            if (!"http".equals(scheme) && !"https".equals(scheme)) {
                throw new IllegalArgumentException("仅支持 http 或 https 地址");
            }
        } else {
            t = "https://" + t;
        }
        while (t.endsWith("/")) {
            t = t.substring(0, t.length() - 1);
        }
        try {
            URI u = URI.create(t);
            String scheme = u.getScheme();
            if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
                throw new IllegalArgumentException("仅支持 http 或 https 地址");
            }
            if (u.getHost() == null || u.getHost().isBlank()) {
                throw new IllegalArgumentException("请填写有效的 CDN 域名或 URL");
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("CDN 地址格式无效", e);
        }
        return t;
    }

    public int getFeedPageSize() {
        String raw = jdbcTemplate.query(
                "SELECT setting_value FROM system_settings WHERE setting_key = ?",
                rs -> rs.next() ? rs.getString(1) : "",
                KEY_FEED_PAGE_SIZE
        );
        return clampFeedPageSize(parseFeedPageSize(raw));
    }

    /**
     * @return stored value after clamp
     */
    @Transactional
    public int setFeedPageSize(Object raw) {
        int n = clampFeedPageSize(parseFeedPageSize(raw == null ? "" : String.valueOf(raw)));
        jdbcTemplate.update(
                """
                        INSERT INTO system_settings (setting_key, setting_value) VALUES (?, ?)
                        ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value
                        """,
                KEY_FEED_PAGE_SIZE,
                String.valueOf(n)
        );
        return n;
    }

    private static int parseFeedPageSize(String raw) {
        if (raw == null || raw.isBlank()) {
            return FEED_PAGE_SIZE_DEFAULT;
        }
        try {
            return Integer.parseInt(raw.trim(), 10);
        } catch (NumberFormatException e) {
            return FEED_PAGE_SIZE_DEFAULT;
        }
    }

    private static int clampFeedPageSize(int n) {
        return Math.max(FEED_PAGE_SIZE_MIN, Math.min(FEED_PAGE_SIZE_MAX, n));
    }
}
