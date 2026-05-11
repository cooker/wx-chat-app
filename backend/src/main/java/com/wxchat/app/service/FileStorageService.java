package com.wxchat.app.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "jfif", "png", "webp");
    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${app.album-image-dir:./uploads/albums}")
    private String albumImageDir;

    public Map<String, Object> uploadImage(MultipartFile file, String folder) throws IOException {
        String originalName = file.getOriginalFilename() == null ? "unknown" : file.getOriginalFilename();
        String ext = getExtension(originalName);
        if (ext.isEmpty() || !ALLOWED_EXTENSIONS.contains(ext)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "仅支持 jpg、jpeg、jfif、png、webp 格式的图片"
            );
        }

        byte[] bytes = file.getBytes();
        if (bytes.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件为空");
        }
        if (bytes.length > MAX_IMAGE_BYTES) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片大小不能超过 10MB");
        }

        BufferedImage decoded;
        try (ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
            decoded = ImageIO.read(in);
        }
        if (decoded == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法解析的图片文件");
        }

        boolean useClientWebp = "webp".equals(ext) && looksLikeRiffWebp(bytes);
        byte[] webpBytes = useClientWebp ? bytes : encodeWebp(normalizeForEncode(decoded));
        String hash = sha256(webpBytes);
        String safeFolder = sanitizeFolder(folder);
        String storageName = hash + ".webp";
        Path dir = getFolderPath(safeFolder);
        Files.createDirectories(dir);
        Path target = dir.resolve(storageName);
        boolean duplicate = Files.exists(target);
        if (!duplicate) {
            Files.write(target, webpBytes);
        }

        String url = toPublicUrl(dir, storageName);
        long id = stableId(url);
        return Map.of(
                "id", id,
                "originalName", originalName,
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
                || name.endsWith(".jfif")
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

    private static boolean looksLikeRiffWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }

    private static BufferedImage normalizeForEncode(BufferedImage src) {
        int type = src.getColorModel().hasAlpha() ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        if (src.getType() == type) {
            return src;
        }
        BufferedImage copy = new BufferedImage(src.getWidth(), src.getHeight(), type);
        Graphics2D g = copy.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            if (!src.getColorModel().hasAlpha() && type == BufferedImage.TYPE_INT_RGB) {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, copy.getWidth(), copy.getHeight());
            }
            g.drawImage(src, 0, 0, null);
        } finally {
            g.dispose();
        }
        return copy;
    }

    private static byte[] encodeWebp(BufferedImage image) throws IOException {
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new IllegalStateException("WebP ImageWriter 未注册，请确认已引入 webp-imageio");
        }
        try (ByteArrayOutputStream raw = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "webp", raw)) {
                throw new IOException("WebP 编码失败");
            }
            return raw.toByteArray();
        }
    }
}
