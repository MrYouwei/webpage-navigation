package com.tanwb.navigation.controller;

import com.tanwb.navigation.common.ApiResponse;
import com.tanwb.navigation.service.WebsiteTitleOperations;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/website")
public class WebsiteTitleController {

    private final WebsiteTitleOperations websiteTitleService;

    public WebsiteTitleController(WebsiteTitleOperations websiteTitleService) {
        this.websiteTitleService = websiteTitleService;
    }

    @GetMapping("/title")
    public ApiResponse<Map<String, String>> getTitle(@RequestParam("url") String url) {
        Map<String, String> result = websiteTitleService.fetchTitle(url);
        String source = result.get("source");
        String message = "fallback".equals(source) ? "获取成功（兜底）" : "获取成功";
        return ApiResponse.ok(message, result);
    }
}
