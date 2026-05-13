package com.wxchat.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ImageCdnSettingsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicConfigShouldExposeImageCdnBase() throws Exception {
        mockMvc.perform(get("/api/public/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.imageCdnBase").exists())
                .andExpect(jsonPath("$.data.uploadServerOrigin").exists())
                .andExpect(jsonPath("$.data.feedPageSize").exists())
                .andExpect(jsonPath("$.data.hotAlbumSize").exists());
    }

    @Test
    void shouldRoundTripFeedPageSize() throws Exception {
        mockMvc.perform(
                        put("/api/admin/settings/feed-page")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"feedPageSize\":12}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedPageSize").value(12));

        mockMvc.perform(get("/api/public/config"))
                .andExpect(jsonPath("$.data.feedPageSize").value("12"));

        mockMvc.perform(
                        put("/api/admin/settings/feed-page")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"feedPageSize\":8}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feedPageSize").value(8));
    }

    @Test
    void shouldRoundTripImageCdnSetting() throws Exception {
        mockMvc.perform(
                        put("/api/admin/settings/image-cdn")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"imageCdnBase\":\"https://cdn.example.com\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.imageCdnBase").value("https://cdn.example.com"));

        mockMvc.perform(get("/api/public/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageCdnBase").value("https://cdn.example.com"));

        mockMvc.perform(
                        put("/api/admin/settings/image-cdn")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"imageCdnBase\":\"\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageCdnBase").value(""));

        mockMvc.perform(get("/api/public/config"))
                .andExpect(jsonPath("$.data.imageCdnBase").value(""));
    }

    @Test
    void shouldRoundTripHotAlbumSize() throws Exception {
        mockMvc.perform(
                        put("/api/admin/settings/hot-album-size")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"hotAlbumSize\":6}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hotAlbumSize").value(6));

        mockMvc.perform(get("/api/public/config"))
                .andExpect(jsonPath("$.data.hotAlbumSize").value("6"));

        mockMvc.perform(
                        put("/api/admin/settings/hot-album-size")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"hotAlbumSize\":8}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hotAlbumSize").value(8));
    }
}
