package com.wxchat.app.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebCorsConfig implements WebMvcConfigurer {

    /** 与 {@code classpath:/static/} 下目录及上传文件对外路径一致 */
    private static final String[] STATIC_RESOURCE_PATTERNS = {
            "/uploads/**",
            "/client/**",
            "/admin/**"
    };

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);

        for (String pattern : STATIC_RESOURCE_PATTERNS) {
            registry.addMapping(pattern)
                    .allowedOriginPatterns("*")
                    .allowedMethods("GET", "HEAD", "OPTIONS")
                    .allowedHeaders("*")
                    .exposedHeaders(
                            HttpHeaders.ACCEPT_RANGES,
                            HttpHeaders.CONTENT_RANGE,
                            HttpHeaders.CONTENT_LENGTH,
                            HttpHeaders.CONTENT_TYPE)
                    .maxAge(3600);
        }
    }
}
