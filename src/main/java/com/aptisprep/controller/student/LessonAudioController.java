package com.aptisprep.controller.student;

import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.presentation.LessonMediaResourceDTO;
import com.aptisprep.presentation.LessonTeachingViewsDTO.HistoricalMediaDTO;
import com.aptisprep.service.learning.LessonTeachingService;
import com.aptisprep.service.storage.StorageService;
import java.security.Principal;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class LessonAudioController {
    private final LessonTeachingService teaching;
    private final StorageService storage;

    public LessonAudioController(LessonTeachingService teaching, StorageService storage) {
        this.teaching = teaching;
        this.storage = storage;
    }

    @GetMapping("/student/lessons/{id}/history/{kind}/{historyId}/media/{media}")
    public ResponseEntity<Resource> historyMedia(
            @PathVariable Integer id,
            @PathVariable String kind,
            @PathVariable Integer historyId,
            @PathVariable String media,
            Principal principal) {
        if (!List.of("attempts", "submissions").contains(kind))
            throw new ResourceNotFoundException("Không tìm thấy lịch sử");
        HistoricalMediaDTO item =
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

    @GetMapping("/lesson-media/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable Integer id, Principal principal) {
        LessonMediaResourceDTO item = teaching.getLessonAudio(id, principal.getName());
        Resource resource = storage.load(item.path());
        return ResponseEntity.ok()
                .contentType(mediaType(item.originalName()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(resource);
    }

    @GetMapping("/student/lessons/{id}/recording")
    public ResponseEntity<Resource> recording(@PathVariable Integer id, Principal principal) {
        String audioPath = teaching.getStudentRecordingPath(id, principal.getName());
        return ResponseEntity.ok()
                .contentType(mediaType(audioPath))
                .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(audioPath));
    }

    @GetMapping("/lesson-media/{id}/image")
    public ResponseEntity<Resource> image(@PathVariable Integer id, Principal principal) {
        String imagePath = teaching.getLessonImagePath(id, principal.getName());
        return ResponseEntity.ok()
                .contentType(imageType(imagePath))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(imagePath));
    }

    @GetMapping("/lesson-media/{id}/video")
    public ResponseEntity<Resource> video(@PathVariable Integer id, Principal principal) {
        String videoPath = teaching.getLessonVideoPath(id, principal.getName());
        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(
                                videoPath.toLowerCase().endsWith(".webm")
                                        ? "video/webm"
                                        : "video/mp4"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(storage.load(videoPath));
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

    private static MediaType mediaType(String name) {
        String n = name == null ? "" : name.toLowerCase();
        if (n.endsWith(".wav")) return MediaType.parseMediaType("audio/wav");
        if (n.endsWith(".ogg")) return MediaType.parseMediaType("audio/ogg");
        if (n.endsWith(".m4a")) return MediaType.parseMediaType("audio/mp4");
        if (n.endsWith(".webm")) return MediaType.parseMediaType("audio/webm");
        return MediaType.parseMediaType("audio/mpeg");
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
