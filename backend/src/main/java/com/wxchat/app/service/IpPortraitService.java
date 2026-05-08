package com.wxchat.app.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class IpPortraitService {
    private static final Pattern IPV4 = Pattern.compile(
            "^(?:(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)\\.){3}(?:25[0-5]|2[0-4]\\d|[01]?\\d\\d?)$"
    );

    /**
     * 宽松 IPv6：降低误拦；仅用于避免把明显的主机名/URL 传给上游。
     */
    private static final Pattern IPV6_LOOSE = Pattern.compile(
            "^[0-9a-fA-F:.]{3,45}$"
    );

    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    public IpPortraitService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> briefInfo(String ip) {
        String normalized = validateAndNormalize(ip);
        String url = "https://qifu.baidu.com/api/v1/ip-portrait/brief-info?ip="
                + URLEncoder.encode(normalized, StandardCharsets.UTF_8);
        URI uri = URI.create(url);

        try {
            String body = restClient.get()
                    .uri(uri)
                    .header("Origin", "https://www.baidu.com")
                    .header("Referer", "https://www.baidu.com/")
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IP 画像接口返回为空");
            }
            return objectMapper.readValue(body, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "IP 画像响应解析失败");
        } catch (RestClientResponseException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "上游接口错误: " + e.getStatusCode().value());
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "网络请求失败");
        }
    }

    private static String validateAndNormalize(String ip) {
        if (ip == null || ip.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ip 不能为空");
        }
        String t = ip.trim();
        if (t.length() > 45 || t.contains("/") || t.contains("\\") || t.contains(" ")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ip 格式不合法");
        }
        if (!IPV4.matcher(t).matches() && !IPV6_LOOSE.matcher(t).matches()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 IPv4 或 IPv6 地址");
        }
        return t;
    }
}
