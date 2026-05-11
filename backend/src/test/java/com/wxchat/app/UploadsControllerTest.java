package com.wxchat.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UploadsControllerTest {

    @TempDir
    static Path tempUploads;

    @DynamicPropertySource
    static void registerUploadDir(DynamicPropertyRegistry registry) {
        registry.add("app.upload-dir", () -> tempUploads.toAbsolutePath().toString());
        registry.add("app.album-image-dir", () -> tempUploads.resolve("albums").toAbsolutePath().toString());
    }

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void writeFixture() throws Exception {
        Path file = tempUploads.resolve("albums/album-serve-test/aa11bb22.webp");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "fake-webp");
    }

    @Test
    void shouldServeFileUnderUploadsAlbums() throws Exception {
        mockMvc.perform(get("/uploads/albums/album-serve-test/aa11bb22.webp"))
                .andExpect(status().isOk());
    }
}
