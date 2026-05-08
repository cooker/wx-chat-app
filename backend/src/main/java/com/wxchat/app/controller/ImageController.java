package com.wxchat.app.controller;

import com.wxchat.app.common.ApiResponse;
import com.wxchat.app.model.Image;
import com.wxchat.app.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize
    ) {
        return ApiResponse.success(Map.of("items", imageService.list(page, pageSize)));
    }

    @GetMapping("/{id}")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<Image> detail(@PathVariable long id) {
        Image image = imageService.getById(id);
        if (image == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
        }
        return ApiResponse.success(image);
    }
}
