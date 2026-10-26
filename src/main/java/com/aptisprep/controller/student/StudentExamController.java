package com.aptisprep.controller.student;

import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.service.account.AccountAccessService;
import com.aptisprep.service.exam.ExamAttemptService;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class StudentExamController {
    private final AccountAccessService accounts;
    private final ExamAttemptService attempts;

    public StudentExamController(AccountAccessService accounts, ExamAttemptService attempts) {
        this.accounts = accounts;
        this.attempts = attempts;
    }

    @PostMapping("/student/exams/{id}/start")
    public String start(
            @PathVariable Integer id, Principal principal, RedirectAttributes redirect) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            return "redirect:/student/attempts/"
                    + attempts.start(
                                    userId,
                                    id,
                                    "exam:" + userId + ":" + id + ":" + UUID.randomUUID())
                            .attemptId();
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/student/exams/" + id;
        }
    }

    @GetMapping("/student/attempts/{id}")
    public String attempt(@PathVariable Integer id, Principal principal, Model model) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        model.addAttribute("attempt", attempts.page(userId, id));
        return "student/attempt";
    }

    @PostMapping("/student/attempts/{id}/answers")
    public String answer(
            @PathVariable Integer id,
            @RequestParam Integer questionId,
            @RequestParam(required = false) Integer itemId,
            @RequestParam(required = false) Integer selectedOptionId,
            @RequestParam(required = false) Integer userPosition,
            @RequestParam(required = false) String textAnswer,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            attempts.saveAnswer(
                    userId, id, questionId, itemId, selectedOptionId, userPosition, textAnswer);
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/attempts/" + id;
    }

    @PostMapping("/student/attempts/{id}/answers/autosave")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> autosave(
            @PathVariable Integer id,
            @RequestParam Integer questionId,
            @RequestParam(required = false) Integer itemId,
            @RequestParam(required = false) Integer selectedOptionId,
            @RequestParam(required = false) Integer userPosition,
            @RequestParam(required = false) String textAnswer,
            Principal principal) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            attempts.saveAnswer(
                    userId, id, questionId, itemId, selectedOptionId, userPosition, textAnswer);
            return ResponseEntity.ok(Map.of("saved", true));
        } catch (BusinessRuleException exception) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("saved", false, "message", exception.getMessage()));
        }
    }

    @PostMapping(
            value = "/student/attempts/{id}/answers/audio",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String audio(
            @PathVariable Integer id,
            @RequestParam Integer questionId,
            @RequestParam(required = false) Integer itemId,
            @RequestParam("audioFile") MultipartFile audioFile,
            Principal principal,
            RedirectAttributes redirect) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            attempts.saveSpeakingAudio(userId, id, questionId, itemId, audioFile);
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/attempts/" + id;
    }

    @PostMapping("/student/attempts/{id}/media/{mediaId}/play")
    @ResponseBody
    public ResponseEntity<ExamAttemptService.MediaPlayResult> playMedia(
            @PathVariable Integer id, @PathVariable Integer mediaId, Principal principal) {
        return ResponseEntity.ok(
                attempts.playMedia(
                        accounts.requireActiveStudent(principal.getName()), id, mediaId));
    }

    @GetMapping("/student/attempts/{id}/media/{mediaId}/content")
    public ResponseEntity<Resource> mediaContent(
            @PathVariable Integer id,
            @PathVariable Integer mediaId,
            @RequestParam(required = false) String token,
            Principal principal) {
        var media =
                attempts.mediaContent(
                        accounts.requireActiveStudent(principal.getName()), id, mediaId, token);
        var response =
                ResponseEntity.ok()
                        .contentType(resolveType(media.contentType()))
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                        .header(HttpHeaders.CACHE_CONTROL, "private, no-store")
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes");
        try {
            response.header(
                    HttpHeaders.CONTENT_LENGTH, String.valueOf(media.resource().contentLength()));
        } catch (IOException ignored) {
            // The resource converter can still stream a readable file without a known length.
        }
        return response.body(media.resource());
    }

    @GetMapping("/student/attempts/{id}/answers/{answerId}/audio")
    public ResponseEntity<Resource> speakingAudio(
            @PathVariable Integer id, @PathVariable Integer answerId, Principal principal) {
        var media =
                attempts.speakingAudio(
                        accounts.requireActiveStudent(principal.getName()), id, answerId);
        return ResponseEntity.ok()
                .contentType(resolveType(media.contentType()))
                .body(media.resource());
    }

    @PostMapping("/student/attempts/{id}/submit")
    public String submit(
            @PathVariable Integer id, Principal principal, RedirectAttributes redirect) {
        try {
            Integer userId = accounts.requireActiveStudent(principal.getName());
            attempts.submit(userId, id, "submit:" + userId + ":" + id);
            return "redirect:/student/attempts/" + id + "/result";
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/student/attempts/" + id;
        }
    }

    private MediaType resolveType(String value) {
        try {
            return value == null
                    ? MediaType.APPLICATION_OCTET_STREAM
                    : MediaType.parseMediaType(value);
        } catch (IllegalArgumentException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
