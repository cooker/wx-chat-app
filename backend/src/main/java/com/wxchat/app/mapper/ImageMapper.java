package com.wxchat.app.mapper;

import com.wxchat.app.model.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ImageMapper {

    @Select("""
            SELECT id, title, image_url, author_name, created_at
            FROM images
            ORDER BY created_at DESC
            LIMIT #{limit} OFFSET #{offset}
            """)
    List<Image> list(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
            SELECT id, title, image_url, author_name, created_at
            FROM images
            WHERE id = #{id}
            """)
    Image findById(@Param("id") long id);
}
