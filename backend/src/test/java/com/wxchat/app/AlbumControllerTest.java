package com.wxchat.app;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AlbumControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldUploadImageWithDeduplication() throws Exception {
        String uniqueContent = "same-image-content-" + UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                uniqueContent.getBytes(StandardCharsets.UTF_8)
        );

        String folder = "test-" + UUID.randomUUID();
        MvcResult firstResult = mockMvc.perform(multipart("/api/files/images").file(file).param("folder", folder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.duplicate").value(false))
                .andReturn();

        MvcResult secondResult = mockMvc.perform(multipart("/api/files/images").file(file).param("folder", folder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.duplicate").value(true))
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertEquals(
                firstJson.path("data").path("id").asLong(),
                secondJson.path("data").path("id").asLong()
        );
    }

    @Test
    void shouldCreateUpdateAndDeleteAlbum() throws Exception {
        MockMultipartFile fileA = new MockMultipartFile(
                "file",
                "a.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "album-image-A".getBytes()
        );
        MockMultipartFile fileB = new MockMultipartFile(
                "file",
                "b.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "album-image-B".getBytes()
        );

        String folder = "album-" + UUID.randomUUID();
        String fileUrlA = uploadAndGetFileUrl(fileA, folder);
        String fileUrlB = uploadAndGetFileUrl(fileB, folder);

        String createBody = """
                {
                  "title": "旅行相册",
                  "description": "重庆夜景记录",
                  "coverUrl": "%s",
                  "imageFolder": "%s"
                }
                """.formatted(fileUrlA, folder);

        MvcResult createResult = mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.title").value("旅行相册"))
                .andExpect(jsonPath("$.data.images.length()").value(2))
                .andReturn();

        long albumId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/albums/" + albumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(albumId))
                .andExpect(jsonPath("$.data.coverUrl").value(fileUrlA));

        mockMvc.perform(get("/api/albums")
                        .param("page", "1")
                        .param("pageSize", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(5))
                .andExpect(jsonPath("$.data.total").isNumber());

        String updateBody = """
                {
                  "title": "旅行相册-更新",
                  "description": "仅保留封面",
                  "coverUrl": "%s",
                  "imageFolder": "%s"
                }
                """.formatted(fileUrlB, folder);

        mockMvc.perform(put("/api/albums/" + albumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("旅行相册-更新"))
                .andExpect(jsonPath("$.data.images.length()").value(2));

        mockMvc.perform(delete("/api/albums/" + albumId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private String uploadAndGetFileUrl(MockMultipartFile file, String folder) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/files/images").file(file).param("folder", folder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("url").asText();
    }
}
