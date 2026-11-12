package com.aptisprep.mvc.support.web;

import com.aptisprep.mvc.service.exam.ExamAttemptService.MediaResource;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;

public final class WebMediaSupport {
    private WebMediaSupport() {}

    public static MediaType audioType(String name) {
        String value = name == null ? "" : name.toLowerCase();
        if (value.endsWith(".wav")) return MediaType.parseMediaType("audio/wav");
        if (value.endsWith(".ogg")) return MediaType.parseMediaType("audio/ogg");
        if (value.endsWith(".m4a")) return MediaType.parseMediaType("audio/mp4");
        if (value.endsWith(".webm")) return MediaType.parseMediaType("audio/webm");
        return MediaType.parseMediaType("audio/mpeg");
    }

    public static MediaType imageType(String name) {
        String value = name == null ? "" : name.toLowerCase();
        if (value.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (value.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (value.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (value.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        return MediaType.IMAGE_JPEG;
    }

    public static MediaType resolveType(String value) {
        try {
            return value == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(value);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    public static ResponseEntity<Resource> inline(MediaResource media) {
        MediaType contentType = resolveType(media.contentType());
        if (!contentType.isConcrete()) {
            contentType =
                    MediaTypeFactory.getMediaType(media.resource())
                            .orElse(MediaType.APPLICATION_OCTET_STREAM);
        }
        ResponseEntity.BodyBuilder response =
                ResponseEntity.ok()
                        .contentType(contentType)
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes");
        try {
            response.header(
                    HttpHeaders.CONTENT_LENGTH, String.valueOf(media.resource().contentLength()));
        } catch (IOException ignored) {
            // Spring vẫn có thể stream resource khi chưa xác định trước độ dài.
        }
        return response.body(media.resource());
    }
}
