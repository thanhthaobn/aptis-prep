package com.aptisprep.mvc.controller;

import com.aptisprep.mvc.dto.LessonDTO;
import com.aptisprep.mvc.exception.ResourceNotFoundException;
import com.aptisprep.mvc.service.learning.LessonService;
import com.aptisprep.mvc.service.learning.LessonTeachingService;
import com.aptisprep.mvc.service.learning.LessonTeachingService.HistoricalMedia;
import com.aptisprep.mvc.service.storage.StorageService;
import com.aptisprep.mvc.support.web.WebMediaSupport;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.List;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LessonAudioController {
    @GetMapping("/student/lessons/{id}/history/{kind}/{historyId}/media/{media}")
    public ResponseEntity<Resource> historyMedia(
            @PathVariable Integer id,
            @PathVariable String kind,
            @PathVariable Integer historyId,
            @PathVariable String media,
            Principal principal) {
        if (!List.of("attempts", "submissions").contains(kind))
            throw new ResourceNotFoundException("Không tìm thấy lịch sử");
        HistoricalMedia item =
                lessonTeachingService.historicalMedia(
                        id, historyId, media, principal.getName(), "attempts".equals(kind));
        MediaType type =
                switch (media) {
                    case "image" -> WebMediaSupport.imageType(item.path());
                    case "audio" -> WebMediaSupport.audioType(item.path());
                    default ->
                            MediaType.parseMediaType(
                                    item.path().toLowerCase().endsWith(".webm")
                                            ? "video/webm"
                                            : "video/mp4");
                };
        return ResponseEntity.ok()
                .contentType(type)
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(storage.load(item.path()));
    }

    private final LessonTeachingService lessonTeachingService;
    private final LessonService lessonService;
    private final StorageService storage;

    public LessonAudioController(
            LessonService lessonService,
            LessonTeachingService lessonTeachingService,
            StorageService storage) {
        this.lessonService = lessonService;
        this.lessonTeachingService = lessonTeachingService;
        this.storage = storage;
    }

    @GetMapping("/lesson-media/{id}/audio")
    public ResponseEntity<?> audio(
            @PathVariable Integer id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
            Principal principal) {
        LessonDTO lesson =
                lessonService.requireForMediaView(
                        id, principal.getName(), "Không tìm thấy âm thanh");
        if (lesson.getAudioPath() == null)
            throw new ResourceNotFoundException("Bài học chưa có âm thanh");
        Resource resource = storage.load(lesson.getAudioPath());
        long contentLength = contentLength(resource);
        MediaType contentType =
                WebMediaSupport.audioType(
                        lesson.getAudioName() == null
                                ? lesson.getAudioPath()
                                : lesson.getAudioName());

        if (rangeHeader == null || rangeHeader.isBlank()) {
            return ResponseEntity.ok()
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(resource);
        }

        List<HttpRange> ranges;
        try {
            ranges = HttpRange.parseRanges(rangeHeader);
        } catch (IllegalArgumentException exception) {
            return rangeNotSatisfiable(contentLength);
        }
        if (ranges.size() != 1) return rangeNotSatisfiable(contentLength);

        HttpRange range = ranges.getFirst();
        long start = range.getRangeStart(contentLength);
        long end = range.getRangeEnd(contentLength);
        if (start < 0 || start >= contentLength || end < start)
            return rangeNotSatisfiable(contentLength);

        long regionLength = end - start + 1;
        Resource region = rangedResource(resource, start, regionLength);
        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .contentType(contentType)
                .contentLength(regionLength)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(
                        HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + contentLength)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(region);
    }

    private long contentLength(Resource resource) {
        try {
            return resource.contentLength();
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Không thể đọc âm thanh");
        }
    }

    private ResponseEntity<Void> rangeNotSatisfiable(long contentLength) {
        return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                .header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
                .build();
    }

    private Resource rangedResource(Resource source, long start, long length) {
        try {
            InputStream input = source.getInputStream();
            skipExactly(input, start);
            InputStream bounded =
                    new InputStream() {
                        private long remaining = length;

                        @Override
                        public int read() throws IOException {
                            if (remaining == 0) return -1;
                            int value = input.read();
                            if (value >= 0) remaining--;
                            return value;
                        }

                        @Override
                        public int read(byte[] bytes, int offset, int amount) throws IOException {
                            if (remaining == 0) return -1;
                            int requested = (int) Math.min(amount, remaining);
                            int read = input.read(bytes, offset, requested);
                            if (read > 0) remaining -= read;
                            return read;
                        }

                        @Override
                        public void close() throws IOException {
                            input.close();
                        }
                    };
            return new InputStreamResource(bounded);
        } catch (IOException exception) {
            throw new ResourceNotFoundException("Không thể đọc âm thanh");
        }
    }

    private void skipExactly(InputStream input, long bytes) throws IOException {
        long remaining = bytes;
        while (remaining > 0) {
            long skipped = input.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            if (input.read() < 0) throw new IOException("Không đủ dữ liệu âm thanh");
            remaining--;
        }
    }

    @GetMapping("/student/lessons/{id}/recording")
    public ResponseEntity<Resource> recording(@PathVariable Integer id, Principal principal) {
        String path = lessonTeachingService.recordingAudio(id, principal.getName());
        return ResponseEntity.ok()
                .contentType(WebMediaSupport.audioType(path))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(path));
    }

    @GetMapping("/lesson-media/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Integer id, Principal principal) {
        LessonDTO lesson =
                lessonService.requireForMediaView(
                        id, principal.getName(), "Không tìm thấy hình ảnh");
        if (lesson.getImagePath() == null)
            throw new ResourceNotFoundException("Bài học chưa có hình ảnh");
        return ResponseEntity.ok()
                .contentType(WebMediaSupport.imageType(lesson.getImagePath()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(lesson.getImagePath()));
    }

    @GetMapping("/lesson-media/{id}/video")
    public ResponseEntity<Resource> video(@PathVariable Integer id, Principal principal) {
        LessonDTO lesson =
                lessonService.requireForMediaView(id, principal.getName(), "Không tìm thấy video");
        if (lesson.getVideoPath() == null)
            throw new ResourceNotFoundException("Không tìm thấy video");
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                lesson.getVideoPath().toLowerCase().endsWith(".webm")
                                        ? "video/webm"
                                        : "video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(lesson.getVideoPath()));
    }

    @GetMapping("/student/lessons/{id}/submissions/{submissionId}/audio")
    public ResponseEntity<Resource> submissionAudio(
            @PathVariable Integer id, @PathVariable Integer submissionId, Principal principal) {
        String path = lessonTeachingService.submissionAudio(id, submissionId, principal.getName());
        return ResponseEntity.ok()
                .contentType(WebMediaSupport.audioType(path))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(storage.load(path));
    }
}
