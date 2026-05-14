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
                .andExpect(jsonPath("$.data.hotAlbumSize").exists())
                .andExpect(jsonPath("$.data.topTitle").exists())
                .andExpect(jsonPath("$.data.topDescription").exists())
                .andExpect(jsonPath("$.data.headerScript").exists());
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

    @Test
    void shouldRoundTripSiteHeader() throws Exception {
        mockMvc.perform(
                        put("/api/admin/settings/site-header")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"topTitle\":\"我的相册\",\"topDescription\":\"记录美好\",\"headerScript\":\"console.log('x')\"}"
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topTitle").value("我的相册"))
                .andExpect(jsonPath("$.data.topDescription").value("记录美好"))
                .andExpect(jsonPath("$.data.headerScript").value("console.log('x')"));

        mockMvc.perform(get("/api/public/config"))
                .andExpect(jsonPath("$.data.topTitle").value("我的相册"))
                .andExpect(jsonPath("$.data.topDescription").value("记录美好"))
                .andExpect(jsonPath("$.data.headerScript").value("console.log('x')"));

        mockMvc.perform(
                        put("/api/admin/settings/site-header")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"topTitle\":\"\",\"topDescription\":\"\",\"headerScript\":\"\"}")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.topTitle").value(""))
                .andExpect(jsonPath("$.data.topDescription").value(""))
                .andExpect(jsonPath("$.data.headerScript").value(""));
    }
}
