package com.tanwb.navigation.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

@Service
public class WebsiteTitleService implements WebsiteTitleOperations {

    private static final Logger log = LoggerFactory.getLogger(WebsiteTitleService.class);
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36";
    private static final int TIMEOUT_MS = 5000;

    @Override
    public Map<String, String> fetchTitle(String url) {
        Map<String, String> result = new HashMap<>();

        try {
            String title = fetchByJsoup(url);
            result.put("title", title);
            result.put("source", "jsoup");
        } catch (Exception e) {
            log.warn("获取网页标题失败: {}, error: {}", url, e.getMessage());
            String fallback = extractFromDomain(url);
            result.put("title", fallback);
            result.put("source", "fallback");
        }

        return result;
    }

    private String fetchByJsoup(String urlStr) throws Exception {
        String normalizedUrl = ensureProtocol(urlStr);

        Document doc = Jsoup.connect(normalizedUrl)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT_MS)
                .followRedirects(true)
                .get();

        String title = doc.title();
        if (title != null && !title.trim().isEmpty()) {
            return title.trim();
        }

        Element ogMeta = doc.selectFirst("meta[property=og:title]");
        if (ogMeta != null) {
            String content = ogMeta.attr("content");
            if (content != null && !content.trim().isEmpty()) {
                return content.trim();
            }
        }

        throw new Exception("无法获取标题");
    }

    private String ensureProtocol(String url) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "https://" + url;
        }
        return url;
    }

    private String extractFromDomain(String urlStr) {
        try {
            String normalizedUrl = ensureProtocol(urlStr);
            URL url = new URL(normalizedUrl);
            String host = url.getHost().toLowerCase();
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            int dotIndex = host.indexOf('.');
            if (dotIndex > 0) {
                String domain = host.substring(0, dotIndex);
                return domain.substring(0, 1).toUpperCase() + domain.substring(1);
            }
            return host;
        } catch (Exception e) {
            return "未知网站";
        }
    }
}
