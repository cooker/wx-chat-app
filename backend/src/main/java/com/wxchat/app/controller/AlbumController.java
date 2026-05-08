package com.wxchat.app.controller;

import com.wxchat.app.common.ApiResponse;
import com.wxchat.app.service.AlbumService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {
    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(albumService.listAlbums(page, pageSize));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable long id) {
        Map<String, Object> album = albumService.getAlbum(id);
        if (album == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found");
        }
        return ApiResponse.success(album);
    }

    @PostMapping
    public ApiResponse<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String title = stringValue(body.get("title"));
        String description = stringValue(body.get("description"));
        String coverUrl = stringValue(body.get("coverUrl"));
        String imageFolder = stringValue(body.get("imageFolder"));
        Long coverFileId = longValue(body.get("coverFileId"));
        return ApiResponse.success(albumService.createAlbum(title, description, coverUrl, imageFolder, coverFileId));
    }

    @PutMapping("/{id}")
    public ApiResponse<Map<String, Object>> update(@PathVariable long id, @RequestBody Map<String, Object> body) {
        String title = stringValue(body.get("title"));
        String description = stringValue(body.get("description"));
        String coverUrl = stringValue(body.get("coverUrl"));
        String imageFolder = stringValue(body.get("imageFolder"));
        Long coverFileId = longValue(body.get("coverFileId"));
        Map<String, Object> updated = albumService.updateAlbum(id, title, description, coverUrl, imageFolder, coverFileId);
        if (updated == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Album not found");
        }
        return ApiResponse.success(updated);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable long id) {
        albumService.deleteAlbum(id);
        return ApiResponse.success(null);
    }

    private static String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private static Long longValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

}
