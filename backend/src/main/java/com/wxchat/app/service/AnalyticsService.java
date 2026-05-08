package com.wxchat.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AnalyticsService {
    private final JdbcTemplate jdbcTemplate;

    public AnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        ensureAccessEventColumns();
    }

    public Map<String, Object> getOverview() {
        long todayStart = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        Long todayPv = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM access_events WHERE viewed_at >= ?",
                Long.class,
                todayStart
        );
        Long todayUv = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT visitor_id) FROM access_events WHERE viewed_at >= ?",
                Long.class,
                todayStart
        );
        Long totalUv = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT visitor_id) FROM access_events",
                Long.class
        );
        Long lastViewedAt = jdbcTemplate.queryForObject(
                "SELECT MAX(viewed_at) FROM access_events",
                Long.class
        );
        List<Map<String, Object>> topAlbums = jdbcTemplate.query(
                """
                        SELECT ae.album_id,
                               COALESCE(a.title, 'Album #' || ae.album_id) AS title,
                               COUNT(*) AS pv,
                               COUNT(DISTINCT ae.visitor_id) AS uv
                        FROM access_events ae
                        LEFT JOIN albums a ON a.id = ae.album_id
                        GROUP BY ae.album_id, a.title
                        ORDER BY pv DESC
                        LIMIT 10
                        """,
                (rs, rowNum) -> Map.of(
                        "albumId", rs.getLong("album_id"),
                        "title", rs.getString("title"),
                        "pv", rs.getLong("pv"),
                        "uv", rs.getLong("uv")
                )
        );
        List<Map<String, Object>> todayDevicePv = jdbcTemplate.query(
                """
                        SELECT CASE
                                   WHEN LOWER(COALESCE(device, '')) LIKE '%iphone%' OR LOWER(COALESCE(device, '')) LIKE '%ios%' THEN 'iPhone'
                                   WHEN LOWER(COALESCE(device, '')) LIKE '%android%' THEN 'Android'
                                   WHEN LOWER(COALESCE(device, '')) LIKE '%windows%' THEN 'Windows'
                                   WHEN LOWER(COALESCE(device, '')) LIKE '%mac%' OR LOWER(COALESCE(device, '')) LIKE '%darwin%' THEN 'Mac'
                                   ELSE 'Other'
                               END AS device,
                               COUNT(*) AS pv
                        FROM access_events
                        WHERE viewed_at >= ?
                        GROUP BY device
                        ORDER BY pv DESC
                        LIMIT 6
                        """,
                (rs, rowNum) -> Map.of(
                        "device", rs.getString("device"),
                        "pv", rs.getLong("pv")
                ),
                todayStart
        );
        return Map.of(
                "todayUv", todayUv == null ? 0L : todayUv,
                "todayPv", todayPv == null ? 0L : todayPv,
                "totalUv", totalUv == null ? 0L : totalUv,
                "lastViewedAt", lastViewedAt == null ? 0L : lastViewedAt,
                "topAlbums", topAlbums,
                "todayDevicePv", todayDevicePv
        );
    }

    public void trackAccess(long albumId, String visitorId, String ip, String userAgent) {
        long now = Instant.now().getEpochSecond();
        String safeVisitorId = (visitorId == null || visitorId.isBlank()) ? "anonymous" : visitorId;
        String safeIp = (ip == null || ip.isBlank()) ? "unknown" : ip;
        String ua = (userAgent == null || userAgent.isBlank()) ? "unknown" : userAgent;
        String safeDevice = normalizeDevice(ua);
        String deviceVersion = extractDeviceVersion(ua);
        jdbcTemplate.update(
                "INSERT INTO access_events (album_id, visitor_id, ip, device, device_version, viewed_at) VALUES (?, ?, ?, ?, ?, ?)",
                albumId,
                safeVisitorId,
                safeIp,
                safeDevice,
                deviceVersion,
                now
        );
    }

    public Map<String, Object> listEvents(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 200));
        int offset = (safePage - 1) * safePageSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM access_events", Long.class);
        List<Map<String, Object>> items = jdbcTemplate.query(
                """
                        SELECT ae.id,
                               ae.album_id,
                               COALESCE(a.title, 'Album #' || ae.album_id) AS album_title,
                               ae.visitor_id,
                               COALESCE(ae.ip, 'unknown') AS ip,
                               COALESCE(ae.device, 'unknown') AS device,
                               COALESCE(ae.device_version, 'unknown') AS device_version,
                               ae.viewed_at
                        FROM access_events ae
                        LEFT JOIN albums a ON a.id = ae.album_id
                        ORDER BY ae.viewed_at DESC, ae.id DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> Map.of(
                        "id", rs.getLong("id"),
                        "albumId", rs.getLong("album_id"),
                        "albumTitle", rs.getString("album_title"),
                        "visitorId", rs.getString("visitor_id"),
                        "ip", rs.getString("ip"),
                        "device", normalizeDevice(rs.getString("device")),
                        "deviceVersion", rs.getString("device_version"),
                        "viewedAt", rs.getLong("viewed_at")
                ),
                safePageSize,
                offset
        );
        return Map.of(
                "items", items,
                "page", safePage,
                "pageSize", safePageSize,
                "total", total == null ? 0L : total
        );
    }

    public void clearEvents() {
        jdbcTemplate.update("DELETE FROM access_events");
    }

    private void ensureAccessEventColumns() {
        addColumnIfMissing("ALTER TABLE access_events ADD COLUMN ip TEXT");
        addColumnIfMissing("ALTER TABLE access_events ADD COLUMN device TEXT");
        addColumnIfMissing("ALTER TABLE access_events ADD COLUMN device_version TEXT");
    }

    private void addColumnIfMissing(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ignored) {
            // Column already exists in existing db, ignore migration error.
        }
    }

    private String normalizeDevice(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Other";
        }
        String value = raw.toLowerCase(Locale.ROOT);
        if (value.contains("iphone") || value.contains("ios")) {
            return "iPhone";
        }
        if (value.contains("android")) {
            return "Android";
        }
        if (value.contains("windows")) {
            return "Windows";
        }
        if (value.contains("mac") || value.contains("darwin")) {
            return "Mac";
        }
        return "Other";
    }

    /**
     * 从 User-Agent 提取可读版本（浏览器 / 系统），用于埋点展示。
     */
    private static String extractDeviceVersion(String ua) {
        if (ua == null || ua.isBlank() || "unknown".equalsIgnoreCase(ua)) {
            return "unknown";
        }
        Matcher m = Pattern.compile("(?:iPhone OS|CPU (?:iPhone )?OS) ([\\d_]+)", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find()) {
            return "iOS " + m.group(1).replace('_', '.');
        }
        m = Pattern.compile("Android ([\\d.]+)", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find()) {
            return "Android " + m.group(1);
        }
        m = Pattern.compile("Chrome/([\\d.]+)", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find() && !ua.contains("Edg/")) {
            return "Chrome " + m.group(1);
        }
        m = Pattern.compile("Edg(?:e|A|IOS)?/([\\d.]+)", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find()) {
            return "Edge " + m.group(1);
        }
        m = Pattern.compile("Firefox/([\\d.]+)", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find()) {
            return "Firefox " + m.group(1);
        }
        m = Pattern.compile("Version/([\\d.]+).*Safari", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find()) {
            return "Safari " + m.group(1);
        }
        m = Pattern.compile("Mac OS X ([\\d_]+)", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find()) {
            return "macOS " + m.group(1).replace('_', '.');
        }
        m = Pattern.compile("Windows NT ([\\d.]+)", Pattern.CASE_INSENSITIVE).matcher(ua);
        if (m.find()) {
            return "Windows " + m.group(1);
        }
        m = Pattern.compile("([A-Za-z][A-Za-z0-9.-]*)/([\\d.]+)\\b").matcher(ua);
        if (m.find()) {
            return m.group(1) + " " + m.group(2);
        }
        return "unknown";
    }
}
