package com.aptisprep.mvc.controller.student;

import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.account.AccountAccessService;
import com.aptisprep.mvc.service.exam.ExamAttemptService;
import com.aptisprep.mvc.service.exam.ExamAttemptService.MediaResource;
import com.aptisprep.mvc.support.web.WebMediaSupport;
import java.security.Principal;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Adapter HTTP cho toàn bộ vòng đời lượt làm bài của học viên.
 *
 * <p>Controller chỉ xử lý xác thực và điều hướng; quyền sở hữu lượt thi, thời gian, snapshot,
 * kiểm tra đáp án và chấm điểm được thực thi trong {@link ExamAttemptService}.
 */
@Controller
public class StudentExamController {
    private final AccountAccessService accounts;
    private final ExamAttemptService attempts;

    public StudentExamController(AccountAccessService accounts, ExamAttemptService attempts) {
        this.accounts = accounts;
        this.attempts = attempts;
    }

    @PostMapping("/student/exams/{id}/start")
    // Request key giúp double-click hoặc retry trả về cùng một lượt thi, không tạo lượt trùng.
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

    @GetMapping("/student/attempts/{id}/review")
    public String review(
            @PathVariable Integer id,
            Principal principal,
            Model model,
            RedirectAttributes redirect) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        try {
            model.addAttribute("review", attempts.review(userId, id));
            model.addAttribute("attemptId", id);
            return "student/attempt/review";
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
            return "redirect:/student/attempts/" + id + "/result";
        }
    }

    @PostMapping("/student/attempts/{id}/discard")
    public String discard(
            @PathVariable Integer id, Principal principal, RedirectAttributes redirect) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        try {
            attempts.discard(userId, id);
            redirect.addFlashAttribute("successMessage", "Đã loại bỏ lượt thi.");
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/results";
    }

    @GetMapping("/student/attempts/{id}")
    public String attempt(@PathVariable Integer id, Principal principal, Model model) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        ExamAttemptService.AttemptPage page = attempts.page(userId, id);
        model.addAttribute("attempt", page);
        return "student/attempt/detail";
    }

    @PostMapping("/student/attempts/{id}/exit")
    public String exitAttempt(
            @PathVariable Integer id, Principal principal, RedirectAttributes redirectAttributes) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        ExamAttemptService.AttemptPage page = attempts.page(userId, id);
        if ("PLACEMENT".equals(page.examType())) {
            String skill =
                    page.sectionSkill() != null
                            ? "FULL_MOCK"
                            : page.questions().isEmpty()
                                    ? "READING"
                                    : page.questions().getFirst().skill();
            redirectAttributes.addAttribute("skill", skill);
            return "redirect:/student/placement";
        }
        return "redirect:/student/exams";
    }

    @PostMapping("/student/attempts/{id}/answers")
    public String answer(
            @PathVariable Integer id,
            @RequestParam(name = "questionId") Integer questionId,
            @RequestParam(name = "itemId", required = false) Integer itemId,
            @RequestParam(name = "selectedOptionId", required = false) Integer selectedOptionId,
            @RequestParam(name = "userPosition", required = false) Integer userPosition,
            @RequestParam(name = "textAnswer", required = false) String textAnswer,
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
    // Autosave dùng cùng luồng nghiệp vụ với submit đáp án, chỉ khác kiểu response HTTP.
    public ResponseEntity<Map<String, Object>> autosave(
            @PathVariable Integer id,
            @RequestParam(name = "questionId") Integer questionId,
            @RequestParam(name = "itemId", required = false) Integer itemId,
            @RequestParam(name = "selectedOptionId", required = false) Integer selectedOptionId,
            @RequestParam(name = "userPosition", required = false) Integer userPosition,
            @RequestParam(name = "textAnswer", required = false) String textAnswer,
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
            @RequestParam(name = "questionId") Integer questionId,
            @RequestParam(name = "itemId", required = false) Integer itemId,
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

    @PostMapping(
            value = "/student/attempts/{id}/answers/audio/autosave",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseBody
    public ResponseEntity<Map<String, Object>> audioAutosave(
            @PathVariable Integer id,
            @RequestParam(name = "questionId") Integer questionId,
            @RequestParam(name = "itemId", required = false) Integer itemId,
            @RequestParam("audioFile") MultipartFile audioFile,
            Principal principal) {
        try {
            attempts.saveSpeakingAudio(
                    accounts.requireActiveStudent(principal.getName()),
                    id,
                    questionId,
                    itemId,
                    audioFile);
            return ResponseEntity.ok(Map.of("saved", true));
        } catch (BusinessRuleException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("saved", false, "message", exception.getMessage()));
        }
    }

    @PostMapping("/student/attempts/{id}/media/{mediaId}/play")
    @ResponseBody
    public ResponseEntity<ExamAttemptService.MediaPlayResult> playMedia(
            @PathVariable Integer id, @PathVariable Integer mediaId, Principal principal) {
        return ResponseEntity.ok(
                attempts.playMedia(
                        accounts.requireActiveStudent(principal.getName()), id, mediaId));
    }

    @GetMapping("/student/attempts/{id}/media/{mediaId}/listen")
    public ResponseEntity<Resource> listenMedia(
            @PathVariable Integer id, @PathVariable Integer mediaId, Principal principal) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        ExamAttemptService.MediaPlayResult permission = attempts.playMedia(userId, id, mediaId);
        return WebMediaSupport.inline(
                attempts.mediaContent(userId, id, mediaId, permission.contentToken()));
    }

    @GetMapping("/student/attempts/{id}/media/{mediaId}/content")
    public ResponseEntity<Resource> mediaContent(
            @PathVariable Integer id,
            @PathVariable Integer mediaId,
            @RequestParam(name = "token", required = false) String token,
            Principal principal) {
        MediaResource media =
                attempts.mediaContent(
                        accounts.requireActiveStudent(principal.getName()), id, mediaId, token);
        return WebMediaSupport.inline(media);
    }

    @GetMapping("/student/attempts/{id}/review/media/{mediaId}")
    public ResponseEntity<Resource> reviewMedia(
            @PathVariable Integer id, @PathVariable Integer mediaId, Principal principal) {
        return WebMediaSupport.inline(
                attempts.reviewMedia(
                        accounts.requireActiveStudent(principal.getName()), id, mediaId));
    }

    @GetMapping("/student/attempts/{id}/review/recording/{questionId}/{itemId}")
    public ResponseEntity<Resource> reviewRecording(
            @PathVariable Integer id,
            @PathVariable Integer questionId,
            @PathVariable Integer itemId,
            Principal principal) {
        return WebMediaSupport.inline(
                attempts.reviewRecording(
                        accounts.requireActiveStudent(principal.getName()),
                        id,
                        questionId,
                        itemId));
    }

    @GetMapping("/student/attempts/{id}/answers/{answerId}/audio")
    public ResponseEntity<Resource> speakingAudio(
            @PathVariable Integer id, @PathVariable Integer answerId, Principal principal) {
        MediaResource media =
                attempts.speakingAudio(
                        accounts.requireActiveStudent(principal.getName()), id, answerId);
        return ResponseEntity.ok()
                .contentType(WebMediaSupport.resolveType(media.contentType()))
                .body(media.resource());
    }

    @PostMapping("/student/attempts/{id}/next-section")
    // Server kiểm tra việc đổi section để client không thể bỏ qua hoặc kéo dài thời gian section.
    public String nextSection(
            @PathVariable Integer id,
            @RequestParam(name = "sectionEndMinute") int sectionEndMinute,
            Principal principal,
            RedirectAttributes redirect) {
        Integer userId = accounts.requireActiveStudent(principal.getName());
        try {
            attempts.advanceSection(userId, id, sectionEndMinute);
        } catch (BusinessRuleException exception) {
            redirect.addFlashAttribute("error", exception.getMessage());
        }
        return "redirect:/student/attempts/" + id;
    }

    @PostMapping("/student/attempts/{id}/submit")
    // Submit khóa lượt thi và khởi động chấm objective/subjective thông qua domain event.
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
}
