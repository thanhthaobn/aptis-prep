package com.aptisprep.controller;

import com.aptisprep.enums.Role;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.repository.LessonResponseRepository;
import com.aptisprep.service.learning.LessonService;
import com.aptisprep.service.storage.StorageService;
import java.security.Principal;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
public class LessonAudioController {
    private final LessonService lessons; private final StorageService storage; private final UserRepository users; private final LessonResponseRepository responses;
    public LessonAudioController(LessonService l,StorageService s,UserRepository u,LessonResponseRepository r){lessons=l;storage=s;users=u;responses=r;}
    @GetMapping("/lesson-media/{id}/audio")
    public ResponseEntity<Resource> audio(@PathVariable Integer id,Principal principal){
        var lesson=lessons.require(id);var user=users.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        if(user.getRole()!=Role.ADMIN&&!"PUBLISHED".equals(lesson.getStatus()))throw new ResourceNotFoundException("Không tìm thấy âm thanh");
        if(lesson.getAudioPath()==null)throw new ResourceNotFoundException("Bài học chưa có âm thanh");
        var resource=storage.load(lesson.getAudioPath());
        return ResponseEntity.ok().contentType(mediaType(lesson.getAudioName())).header(HttpHeaders.CONTENT_DISPOSITION,"inline").body(resource);
    }
    @GetMapping("/student/lessons/{id}/recording")
    public ResponseEntity<Resource> recording(@PathVariable Integer id,Principal principal){var user=users.findByEmailIgnoreCase(principal.getName()).orElseThrow();var work=responses.findByUserIdAndLessonId(user.getId(),id).filter(r->r.getAudioPath()!=null).orElseThrow(()->new ResourceNotFoundException("Chưa có bản ghi"));return ResponseEntity.ok().contentType(MediaType.parseMediaType("audio/webm")).header(HttpHeaders.CONTENT_DISPOSITION,"inline").body(storage.load(work.getAudioPath()));}
    private static MediaType mediaType(String name){String n=name==null?"":name.toLowerCase();if(n.endsWith(".wav"))return MediaType.parseMediaType("audio/wav");if(n.endsWith(".ogg"))return MediaType.parseMediaType("audio/ogg");if(n.endsWith(".m4a"))return MediaType.parseMediaType("audio/mp4");if(n.endsWith(".webm"))return MediaType.parseMediaType("audio/webm");return MediaType.parseMediaType("audio/mpeg");}
}
