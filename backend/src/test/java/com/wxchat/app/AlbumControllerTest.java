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

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
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
        byte[] png = tinyPng((0xAABBCC ^ UUID.randomUUID().hashCode()) & 0xFFFFFF);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                MediaType.IMAGE_PNG_VALUE,
                png
        );

        String folder = "test-" + UUID.randomUUID();
        MvcResult firstResult = mockMvc.perform(multipart("/api/files/images").file(file).param("folder", folder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.duplicate").value(false))
                .andExpect(jsonPath("$.data.url").value(endsWith(".webp")))
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
    void shouldRejectUnsupportedImageExtension() throws Exception {
        byte[] png = tinyPng(0x112233);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "x.gif",
                MediaType.IMAGE_GIF_VALUE,
                png
        );
        mockMvc.perform(multipart("/api/files/images").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldCreateUpdateAndDeleteAlbum() throws Exception {
        MockMultipartFile fileA = new MockMultipartFile(
                "file",
                "a.png",
                MediaType.IMAGE_PNG_VALUE,
                tinyPng(0xFF0000)
        );
        MockMultipartFile fileB = new MockMultipartFile(
                "file",
                "b.png",
                MediaType.IMAGE_PNG_VALUE,
                tinyPng(0x0000FF)
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

    @Test
    void shouldListHotAlbumsByViews() throws Exception {
        mockMvc.perform(delete("/api/analytics/events"))
                .andExpect(status().isOk());

        String folderA = "hot-a-" + UUID.randomUUID();
        String folderB = "hot-b-" + UUID.randomUUID();
        String coverA = uploadAndGetFileUrl(new MockMultipartFile(
                "file",
                "hot-a.png",
                MediaType.IMAGE_PNG_VALUE,
                tinyPng(0x00AA11)
        ), folderA);
        String coverB = uploadAndGetFileUrl(new MockMultipartFile(
                "file",
                "hot-b.png",
                MediaType.IMAGE_PNG_VALUE,
                tinyPng(0x1100AA)
        ), folderB);

        long albumA = createAlbumAndGetId("热门A", folderA, coverA);
        long albumB = createAlbumAndGetId("热门B", folderB, coverB);

        track(albumA, "visitor-a");
        track(albumA, "visitor-b");
        track(albumA, "visitor-c");
        track(albumB, "visitor-d");

        mockMvc.perform(get("/api/albums/hot").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items[0].id").value(albumA))
                .andExpect(jsonPath("$.data.items[0].views").value(3))
                .andExpect(jsonPath("$.data.items[1].id").value(albumB))
                .andExpect(jsonPath("$.data.items[1].views").value(1));
    }

    private String uploadAndGetFileUrl(MockMultipartFile file, String folder) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/files/images").file(file).param("folder", folder))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.url").value(endsWith(".webp")))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("url").asText();
    }

    private long createAlbumAndGetId(String title, String folder, String coverUrl) throws Exception {
        String body = """
                {
                  "title": "%s",
                  "description": "desc",
                  "coverUrl": "%s",
                  "imageFolder": "%s"
                }
                """.formatted(title, coverUrl, folder);
        MvcResult createResult = mockMvc.perform(post("/api/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();
    }

    private void track(long albumId, String visitorId) throws Exception {
        String body = """
                {
                  "albumId": %d,
                  "visitorId": "%s"
                }
                """.formatted(albumId, visitorId);
        mockMvc.perform(post("/api/analytics/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private static byte[] tinyPng(int rgb) throws IOException {
        BufferedImage img = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        int argb = 0xFF000000 | (rgb & 0xFFFFFF);
        for (int x = 0; x < 2; x++) {
            for (int y = 0; y < 2; y++) {
                img.setRGB(x, y, argb);
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "png", baos);
        return baos.toByteArray();
    }
}
