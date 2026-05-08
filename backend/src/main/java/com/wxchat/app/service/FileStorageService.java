package com.wxchat.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service
public class FileStorageService {
    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.album-image-dir:./uploads/albums}")
    private String albumImageDir;

    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        byte[] bytes = file.getBytes();
        String hash = sha256(bytes);
        String safeFolder = sanitizeFolder(folder);
        String ext = getExtension(file.getOriginalFilename());
        String storageName = ext.isEmpty() ? hash : hash + "." + ext;
        Path dir = getFolderPath(safeFolder);
        Files.createDirectories(dir);
        Path target = dir.resolve(storageName);
        boolean duplicate = Files.exists(target);
        if (!duplicate) {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        }

        String url = toPublicUrl(dir, storageName);
        long id = stableId(url);
        return Map.of(
                "id", id,
                "originalName", file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename(),
                "url", url,
                "hash", hash,
                "folder", safeFolder,
                "duplicate", duplicate
        );
    }

    public List<Map<String, Object>> listIndexedImages(String folder) {
        String safeFolder = sanitizeFolder(folder);
        Path dir = getFolderPath(safeFolder);
        if (!Files.exists(dir) || !Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(FileStorageService::isImageFile)
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed())
                    .map(path -> {
                        String url = toPublicUrl(dir, path.getFileName().toString());
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", stableId(url));
                        item.put("originalName", path.getFileName().toString());
                        item.put("url", url);
                        return item;
                    })
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to index image folder: " + safeFolder, e);
        }
    }

    public String resolveUrlById(long id) {
        Path base = Path.of(albumImageDir).toAbsolutePath().normalize();
        if (!Files.exists(base)) {
            return "";
        }
        try (Stream<Path> stream = Files.walk(base, 4)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(FileStorageService::isImageFile)
                    .map(path -> {
                        Path parent = path.getParent() == null ? base : path.getParent();
                        String url = toPublicUrl(parent, path.getFileName().toString());
                        return Map.of("id", stableId(url), "url", url);
                    })
                    .filter(item -> ((Long) item.get("id")) == id)
                    .map(item -> String.valueOf(item.get("url")))
                    .findFirst()
                    .orElse("");
        } catch (IOException e) {
            return "";
        }
    }

    public String defaultAlbumFolder(long albumId) {
        return "album-" + albumId;
    }

    private Path getFolderPath(String folder) {
        Path base = Path.of(albumImageDir).toAbsolutePath().normalize();
        return base.resolve(folder).normalize();
    }

    private String sanitizeFolder(String folder) {
        String value = folder == null || folder.isBlank() ? "default" : folder.trim();
        value = value.replace("\\", "/");
        if (value.startsWith("/")) {
            value = value.substring(1);
        }
        Path normalized = Paths.get(value).normalize();
        String result = normalized.toString().replace("\\", "/");
        if (result.isBlank() || result.startsWith("..")) {
            return "default";
        }
        return result;
    }

    private String toPublicUrl(Path folderPath, String filename) {
        Path uploadBase = Path.of(uploadDir).toAbsolutePath().normalize();
        Path target = folderPath.resolve(filename).toAbsolutePath().normalize();
        Path relative = uploadBase.relativize(target);
        return "/uploads/" + relative.toString().replace("\\", "/");
    }

    private static long stableId(String value) {
        return Integer.toUnsignedLong(value.hashCode());
    }

    private static boolean isImageFile(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".webp")
                || name.endsWith(".gif")
                || name.endsWith(".heic");
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private static String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
