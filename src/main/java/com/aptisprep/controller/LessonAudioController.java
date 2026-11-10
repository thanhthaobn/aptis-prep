package com.aptisprep.controller;

import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonResponse;
import com.aptisprep.entity.User;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.mvc.storage.service.StorageService;
import com.aptisprep.repository.LessonResponseRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.learning.LessonService;
import com.aptisprep.service.learning.LessonTeachingService;
import com.aptisprep.service.learning.LessonTeachingService.HistoricalMedia;
import java.security.Principal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
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
                teaching.historicalMedia(
                        id, historyId, media, principal.getName(), "attempts".equals(kind));
        MediaType type =
                switch (media) {
                    case "image" -> imageType(item.path());
                    case "audio" -> mediaType(item.path());
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

    @Autowired private LessonTeachingService teaching;
    private final LessonService lessons;
    private final StorageService storage;
    private final UserRepository userRepository;
    private final LessonResponseRepository lessonResponseRepository;

    public LessonAudioController(
            LessonService l,
            StorageService s,
            UserRepository userRepository,
            LessonResponseRepository lessonResponseRepository) {
        lessons = l;
        storage = s;
        this.userRepository = userRepository;
        this.lessonResponseRepository = lessonResponseRepository;
    }

    @GetMapping("/lesson-media/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable Integer id, Principal principal) {
        Lesson lesson = lessons.require(id);
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        if (user.getRole() != Role.ADMIN && !"PUBLISHED".equals(lesson.getStatus()))
            throw new ResourceNotFoundException("Không tìm thấy âm thanh");
        if (lesson.getAudioPath() == null)
            throw new ResourceNotFoundException("Bài học chưa có âm thanh");
        Resource resource = storage.load(lesson.getAudioPath());
        return ResponseEntity.ok()
                .contentType(mediaType(lesson.getAudioName()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @GetMapping("/student/lessons/{id}/recording")
    public ResponseEntity<Resource> recording(@PathVariable Integer id, Principal principal) {
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        LessonResponse work =
                lessonResponseRepository
                        .findByUserIdAndLessonId(user.getId(), id)
                        .filter(r -> r.getAudioPath() != null)
                        .orElseThrow(() -> new ResourceNotFoundException("Chưa có bản ghi"));
        return ResponseEntity.ok()
                .contentType(mediaType(work.getAudioPath()))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(work.getAudioPath()));
    }

    @GetMapping("/lesson-media/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Integer id, Principal principal) {
        Lesson lesson = lessons.require(id);
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        if (user.getRole() != Role.ADMIN && !"PUBLISHED".equals(lesson.getStatus()))
            throw new ResourceNotFoundException("Không tìm thấy hình ảnh");
        if (lesson.getImagePath() == null)
            throw new ResourceNotFoundException("Bài học chưa có hình ảnh");
        return ResponseEntity.ok()
                .contentType(imageType(lesson.getImagePath()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(lesson.getImagePath()));
    }

    private static MediaType mediaType(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".wav")) return MediaType.parseMediaType("audio/wav");
        if (n.endsWith(".ogg")) return MediaType.parseMediaType("audio/ogg");
        if (n.endsWith(".m4a")) return MediaType.parseMediaType("audio/mp4");
        if (n.endsWith(".webm")) return MediaType.parseMediaType("audio/webm");
        return MediaType.parseMediaType("audio/mpeg");
    }

    @GetMapping("/lesson-media/{id}/video")
    public ResponseEntity<Resource> video(@PathVariable Integer id, Principal principal) {
        Lesson lesson = lessons.require(id);
        User user = userRepository.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        if (user.getRole() != Role.ADMIN && !"PUBLISHED".equals(lesson.getStatus())
                || lesson.getVideoPath() == null)
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
        String path = teaching.submissionAudio(id, submissionId, principal.getName());
        return ResponseEntity.ok()
                .contentType(mediaType(path))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .body(storage.load(path));
    }

    private static MediaType imageType(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (n.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (n.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (n.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        return MediaType.IMAGE_JPEG;
    }
}
