package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonCheckDTO;
import com.aptisprep.entity.LessonResponse;
import com.aptisprep.entity.LessonWorkSubmission;
import com.aptisprep.presentation.LessonMediaResourceDTO;
import com.aptisprep.presentation.LessonResponseDTO;
import com.aptisprep.presentation.LessonTeachingViewsDTO.*;
import com.aptisprep.presentation.LessonWorkSubmissionDTO;
import com.aptisprep.presentation.form.LessonEditorForm;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface LessonTeachingService {
    ReleaseStateDTO releaseState(Integer id, String email);

    boolean canAssess(Integer id, String email);

    boolean aiAvailable();

    HistoricalMediaDTO historicalMedia(
            Integer lessonId, Integer historyId, String kind, String email, boolean quiz);

    void resetAudioEngagement(Integer id);

    LessonResponse savedWork(Integer lessonId, String email);

    LessonResponseDTO savedWorkDto(Integer lessonId, String email);

    void saveRecording(Integer lessonId, MultipartFile file, String email);

    String message(String skill);

    List<AttemptViewDTO> history(Integer lessonId, String email);

    List<LessonWorkSubmission> submissionHistory(Integer lessonId, String email);

    List<LessonWorkSubmissionDTO> submissionHistoryDto(Integer lessonId, String email);

    List<LessonWorkSubmission> assessableSubmissions(Integer lessonId, String email);

    List<LessonWorkSubmissionDTO> assessableSubmissionsDto(Integer lessonId, String email);

    String submissionAudio(Integer lessonId, Integer submissionId, String email);

    void submitResponse(Integer lessonId, String text, String email);

    CompletionStateDTO completion(Integer lessonId, String email);

    void section(Integer lessonId, String section, String email);

    String lastSection(Integer lessonId, String email);

    LessonStatsDTO stats(Integer lessonId);

    String response(Integer lessonId, String email);

    void saveResponse(Integer lessonId, String text, String email);

    String assess(Integer lessonId, Integer submissionId, String email);

    List<CheckViewDTO> questions(Integer id);

    List<CheckViewDTO> questions(Integer id, String email);

    List<LessonCheckDTO> editorQuestions(Integer id);

    void validate(LessonEditorForm f);

    void saveChecks(Integer id, List<LessonCheckDTO> forms);

    FeedbackDTO grade(Integer id, Map<String, String> answers, String email);

    LessonMediaResourceDTO getLessonAudio(Integer lessonId, String email);

    String getStudentRecordingPath(Integer lessonId, String email);

    String getLessonImagePath(Integer lessonId, String email);

    String getLessonVideoPath(Integer lessonId, String email);

    String videoId(String videoUrl);
}
