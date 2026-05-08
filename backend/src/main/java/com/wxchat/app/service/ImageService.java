package com.wxchat.app.service;

import com.wxchat.app.mapper.ImageMapper;
import com.wxchat.app.model.Image;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ImageService {
    private final ImageMapper imageMapper;

    public ImageService(ImageMapper imageMapper) {
        this.imageMapper = imageMapper;
    }

    public List<Image> list(int page, int pageSize) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int offset = (safePage - 1) * safePageSize;
        return imageMapper.list(safePageSize, offset);
    }

    public Image getById(long id) {
        return imageMapper.findById(id);
    }
}
