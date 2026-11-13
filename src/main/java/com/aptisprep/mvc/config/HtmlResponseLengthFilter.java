package com.aptisprep.mvc.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/** Sends a fixed length for HTML pages so embedded browsers do not reject chunked responses. */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HtmlResponseLengthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String accept = request.getHeader("Accept");
        if (!"GET".equalsIgnoreCase(request.getMethod())
                || accept == null
                || !accept.contains("text/html")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingResponseWrapper wrapped = new ContentCachingResponseWrapper(response);
        filterChain.doFilter(request, wrapped);

        byte[] body = wrapped.getContentAsByteArray();
        String contentType = wrapped.getContentType();
        if (contentType != null && contentType.toLowerCase(java.util.Locale.ROOT).contains("text/html")) {
            wrapped.setContentLength(body.length);
        }
        wrapped.copyBodyToResponse();
    }
}
