package com.tanwb.navigation.config;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);
    private static final int MAX_BODY_LENGTH = 4000;
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(\"(?:password|confirmPassword)\"\\s*:\\s*\")([^\"]*)(\")",
            Pattern.CASE_INSENSITIVE);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, java.io.IOException {
        long start = System.currentTimeMillis();
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String requestBody = redact(readRequestBody(requestWrapper));
            String responseBody = redact(readResponseBody(responseWrapper));

            log.info("HTTP {} {} status={} duration={}ms requestBody={} responseBody={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    responseWrapper.getStatus(),
                    duration,
                    requestBody,
                    responseBody);

            responseWrapper.copyBodyToResponse();
        }
    }

    private String readRequestBody(ContentCachingRequestWrapper request) {
        return readBody(request.getContentAsByteArray(), request.getCharacterEncoding());
    }

    private String readResponseBody(ContentCachingResponseWrapper response) {
        return readBody(response.getContentAsByteArray(), response.getCharacterEncoding());
    }

    private String readBody(byte[] bytes, String encoding) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        String charset = encoding == null ? "UTF-8" : encoding;
        try {
            return limit(new String(bytes, charset).replaceAll("[\\r\\n\\t]+", " ").trim());
        } catch (UnsupportedEncodingException ex) {
            return "[unsupported encoding: " + charset + "]";
        }
    }

    private String redact(String body) {
        if (body == null || body.isEmpty()) {
            return "";
        }
        return PASSWORD_PATTERN.matcher(body).replaceAll("$1******$3");
    }

    private String limit(String body) {
        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }
        return body.substring(0, MAX_BODY_LENGTH) + "...[truncated]";
    }
}
