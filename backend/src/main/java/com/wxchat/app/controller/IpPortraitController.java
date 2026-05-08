package com.wxchat.app.controller;

import com.wxchat.app.common.ApiResponse;
import com.wxchat.app.service.IpPortraitService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/tools")
public class IpPortraitController {
    private final IpPortraitService ipPortraitService;

    public IpPortraitController(IpPortraitService ipPortraitService) {
        this.ipPortraitService = ipPortraitService;
    }

    /**
     * 代理奇符 IP 画像接口（管理端工具）。浏览器直连受 CORS / 受限请求头影响，故走后端转发。
     * 上游：https://qifu.baidu.com/api/v1/ip-portrait/brief-info?ip=
     */
    @GetMapping("/ip-portrait")
    public ApiResponse<Map<String, Object>> briefInfo(@RequestParam("ip") String ip) {
        return ApiResponse.success(ipPortraitService.briefInfo(ip));
    }
}
