package com.wxchat.app;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturnAnalyticsOverview() throws Exception {
        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.todayUv").isNumber())
                .andExpect(jsonPath("$.data.todayPv").isNumber())
                .andExpect(jsonPath("$.data.totalUv").isNumber())
                .andExpect(jsonPath("$.data.lastViewedAt").isNumber())
                .andExpect(jsonPath("$.data.topAlbums").isArray())
                .andExpect(jsonPath("$.data.todayDevicePv").isArray());
    }

    @Test
    void shouldTrackAlbumAccessEvent() throws Exception {
        MvcResult before = mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andReturn();

        int beforePv = com.jayway.jsonpath.JsonPath.read(
                before.getResponse().getContentAsString(),
                "$.data.todayPv"
        );

        mockMvc.perform(post("/api/analytics/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "8.8.8.8")
                        .header("User-Agent", "UnitTestBrowser/1.0")
                        .content("""
                                {
                                  "albumId": 1,
                                  "visitorId": "test-visitor-track"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/analytics/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.todayPv").value(beforePv + 1));
    }

    @Test
    void shouldListAndClearEvents() throws Exception {
        mockMvc.perform(post("/api/analytics/track")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Forwarded-For", "9.9.9.9")
                        .header("User-Agent", "AnalyticsTestDevice/2.0")
                        .content("""
                                {
                                  "albumId": 1,
                                  "visitorId": "test-event-list"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/analytics/events")
                        .param("page", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.items[0].ip").exists())
                .andExpect(jsonPath("$.data.items[0].device").value(
                        org.hamcrest.Matchers.anyOf(
                                org.hamcrest.Matchers.is("iPhone"),
                                org.hamcrest.Matchers.is("Android"),
                                org.hamcrest.Matchers.is("Windows"),
                                org.hamcrest.Matchers.is("Mac"),
                                org.hamcrest.Matchers.is("Other")
                        )
                ))
                .andExpect(jsonPath("$.data.items[0].deviceVersion").exists());

        mockMvc.perform(delete("/api/analytics/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/api/analytics/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }
}
