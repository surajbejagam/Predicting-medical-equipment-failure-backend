package com.example.predict;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Enumeration;
import java.util.stream.Collectors;

@Component
public class RequestResponseLoggingFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RequestResponseLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);
        long start = System.currentTimeMillis();
        try {
            logger.info("Incoming Request: {} {}", wrappedRequest.getMethod(), wrappedRequest.getRequestURI());
            logger.info("Headers: {}", Collections.list(wrappedRequest.getHeaderNames()).stream()
                    .collect(Collectors.toMap(h -> h, wrappedRequest::getHeader)));
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - start;
            String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            logger.info("Request Body: {}", requestBody);
            logger.info("Response Status: {} for {} {} in {}ms", wrappedResponse.getStatus(), wrappedRequest.getMethod(), wrappedRequest.getRequestURI(), duration);
            logger.info("Response Body: {}", responseBody);
            wrappedResponse.copyBodyToResponse();
        }
    }
}
