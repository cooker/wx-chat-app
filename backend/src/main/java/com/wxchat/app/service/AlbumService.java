package com.wxchat.app.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class AlbumService {
    private final JdbcTemplate jdbcTemplate;
    private final FileStorageService fileStorageService;

    public AlbumService(JdbcTemplate jdbcTemplate, FileStorageService fileStorageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.fileStorageService = fileStorageService;
        ensureAlbumColumns();
    }

    public Map<String, Object> listAlbums(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, Math.min(pageSize, 100));
        int offset = (safePage - 1) * safePageSize;
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM albums", Long.class);
        List<Map<String, Object>> rows = jdbcTemplate.query(
                """
                        SELECT a.id, a.title, a.description,
                               COALESCE(a.cover_url, '') AS cover_url,
                               COALESCE(a.image_folder, '') AS image_folder,
                               a.created_at, a.updated_at
                        FROM albums a
                        ORDER BY a.updated_at DESC
                        LIMIT ? OFFSET ?
                        """,
                (rs, rowNum) -> Map.of(
                        "id", rs.getLong("id"),
                        "title", rs.getString("title"),
                        "description", rs.getString("description"),
                        "coverUrl", rs.getString("cover_url"),
                        "imageFolder", rs.getString("image_folder"),
                        "createdAt", rs.getLong("created_at"),
                        "updatedAt", rs.getLong("updated_at")
                ),
                safePageSize,
                offset
        );
        List<Map<String, Object>> items = rows.stream().map(row -> {
            String folder = String.valueOf(row.get("imageFolder"));
            long imageCount = fileStorageService.listIndexedImages(folder).size();
            return Map.of(
                    "id", row.get("id"),
                    "title", row.get("title"),
                    "description", row.get("description"),
                    "coverUrl", row.get("coverUrl"),
                    "imageFolder", row.get("imageFolder"),
                    "imageCount", imageCount,
                    "createdAt", row.get("createdAt"),
                    "updatedAt", row.get("updatedAt")
            );
        }).toList();
        return Map.of(
                "items", items,
                "page", safePage,
                "pageSize", safePageSize,
                "total", total == null ? 0L : total
        );
    }

    public Map<String, Object> getAlbum(long id) {
        Map<String, Object> album = jdbcTemplate.query(
                """
                        SELECT a.id, a.title, a.description,
                               COALESCE(a.cover_url, '') AS cover_url,
                               COALESCE(a.image_folder, '') AS image_folder,
                               a.created_at, a.updated_at
                        FROM albums a
                        WHERE a.id = ?
                        """,
                rs -> rs.next()
                        ? Map.of(
                        "id", rs.getLong("id"),
                        "title", rs.getString("title"),
                        "description", rs.getString("description"),
                        "coverUrl", rs.getString("cover_url"),
                        "imageFolder", rs.getString("image_folder"),
                        "createdAt", rs.getLong("created_at"),
                        "updatedAt", rs.getLong("updated_at")
                )
                        : null,
                id
        );
        if (album == null) {
            return null;
        }
        List<Map<String, Object>> images = fileStorageService.listIndexedImages(String.valueOf(album.get("imageFolder")));
        return Map.of(
                "id", album.get("id"),
                "title", album.get("title"),
                "description", album.get("description"),
                "coverUrl", album.get("coverUrl"),
                "imageFolder", album.get("imageFolder"),
                "createdAt", album.get("createdAt"),
                "updatedAt", album.get("updatedAt"),
                "images", images
        );
    }

    @Transactional
    public Map<String, Object> createAlbum(String title, String description, String coverUrl, String imageFolder, Long coverFileId) {
        long now = Instant.now().getEpochSecond();
        String resolvedCoverUrl = resolveCoverUrl(coverUrl, coverFileId);
        jdbcTemplate.update(
                "INSERT INTO albums (title, description, cover_url, image_folder, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)",
                title, description, resolvedCoverUrl, safeFolder(imageFolder), now, now
        );
        Long albumId = jdbcTemplate.queryForObject("SELECT id FROM albums ORDER BY id DESC LIMIT 1", Long.class);
        if (imageFolder == null || imageFolder.isBlank()) {
            String defaultFolder = fileStorageService.defaultAlbumFolder(albumId);
            jdbcTemplate.update("UPDATE albums SET image_folder = ? WHERE id = ?", defaultFolder, albumId);
        }
        return getAlbum(albumId);
    }

    @Transactional
    public Map<String, Object> updateAlbum(long albumId, String title, String description, String coverUrl, String imageFolder, Long coverFileId) {
        long now = Instant.now().getEpochSecond();
        String resolvedCoverUrl = resolveCoverUrl(coverUrl, coverFileId);
        jdbcTemplate.update(
                "UPDATE albums SET title = ?, description = ?, cover_url = ?, image_folder = ?, updated_at = ? WHERE id = ?",
                title, description, resolvedCoverUrl, safeFolder(imageFolder), now, albumId
        );
        return getAlbum(albumId);
    }

    @Transactional
    public void deleteAlbum(long albumId) {
        jdbcTemplate.update("DELETE FROM albums WHERE id = ?", albumId);
    }

    private String resolveCoverUrl(String coverUrl, Long coverFileId) {
        if (coverUrl != null && !coverUrl.isBlank()) {
            return coverUrl;
        }
        if (coverFileId != null) {
            String resolved = fileStorageService.resolveUrlById(coverFileId);
            if (!resolved.isBlank()) {
                return resolved;
            }
        }
        return "";
    }

    private String safeFolder(String imageFolder) {
        return imageFolder == null ? "" : imageFolder.trim();
    }

    private void ensureAlbumColumns() {
        addColumnIfMissing("ALTER TABLE albums ADD COLUMN cover_url TEXT NOT NULL DEFAULT ''");
        addColumnIfMissing("ALTER TABLE albums ADD COLUMN image_folder TEXT NOT NULL DEFAULT ''");
    }

    private void addColumnIfMissing(String sql) {
        try {
            jdbcTemplate.execute(sql);
        } catch (DataAccessException ignored) {
            // Column already exists.
        }
    }
}
