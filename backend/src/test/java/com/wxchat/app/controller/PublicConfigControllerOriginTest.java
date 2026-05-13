package com.wxchat.app.controller;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PublicConfigControllerOriginTest {

    @Test
    void shouldPreferConfiguredOverride() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        assertEquals(
                "https://api.example.com",
                PublicConfigController.resolveUploadServerOrigin(req, "https://api.example.com/")
        );
    }

    @Test
    void shouldBuildFromForwardedHeaders() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-Proto", "https");
        req.addHeader("X-Forwarded-Host", "cdn-front.example.com, other");
        assertEquals(
                "https://cdn-front.example.com",
                PublicConfigController.resolveUploadServerOrigin(req, "")
        );
    }

    @Test
    void shouldFallbackToServerNameAndPort() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerName("127.0.0.1");
        req.setServerPort(8384);
        assertEquals(
                "http://127.0.0.1:8384",
                PublicConfigController.resolveUploadServerOrigin(req, "")
        );
    }
}
