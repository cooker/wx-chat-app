package com.wxchat.app.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/** Serves {@code app.upload-dir} at {@code /uploads/**}; avoids default {@code /**} classpath static mapping 404ing paths that only exist on disk. */
@RestController
public class UploadsController {

    private final Path uploadRoot;

    public UploadsController(@Value("${app.upload-dir:./uploads}") String uploadDir) {
        this.uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    @GetMapping("/uploads/{*relativePath}")
    public ResponseEntity<Resource> serve(@PathVariable String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        // 去掉前导 /，避免 Path.resolve("/a/b") 被当成绝对路径而脱离 uploadRoot
        String normalizedRelative = relativePath.replace('\\', '/').replaceFirst("^/+", "");
        if (normalizedRelative.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        Path resolved = uploadRoot.resolve(normalizedRelative).normalize();
        if (!resolved.startsWith(uploadRoot)) {
            return ResponseEntity.notFound().build();
        }
        if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(resolved);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = probeMediaType(resolved);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePublic())
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .body(resource);
    }

    private static MediaType probeMediaType(Path path) {
        try {
            String ct = Files.probeContentType(path);
            if (ct != null && !ct.isBlank()) {
                return MediaType.parseMediaType(ct);
            }
        } catch (IOException ignored) {
            // fall through to extension
        }
        String name = path.getFileName().toString().toLowerCase();
        if (name.endsWith(".heic") || name.endsWith(".heif")) {
            return MediaType.parseMediaType("image/heic");
        }
        if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".jfif")) {
            return MediaType.IMAGE_JPEG;
        }
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
