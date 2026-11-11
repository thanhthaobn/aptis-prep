package com.aptisprep.service.learning;

import com.aptisprep.dto.LessonCheckDTO;
import com.aptisprep.dto.LessonEditorForm;
import com.aptisprep.dto.LessonMediaResourceDTO;
import com.aptisprep.dto.LessonResponseDTO;
import com.aptisprep.dto.LessonTeachingViewsDTO.*;
import com.aptisprep.dto.LessonWorkSubmissionDTO;
import com.aptisprep.entity.LessonResponse;
import com.aptisprep.entity.LessonWorkSubmission;
import java.util.List;
import java.util.Map;
import org.springframework.web.multipart.MultipartFile;

public interface LessonTeachingService {
    public ReleaseStateDTO releaseState(Integer id, String email);

    public boolean canAssess(Integer id, String email);

    public boolean aiAvailable();

    public HistoricalMediaDTO historicalMedia(
            Integer lessonId, Integer historyId, String kind, String email, boolean quiz);

    public void resetAudioEngagement(Integer id);

    public LessonResponse savedWork(Integer lessonId, String email);

    public LessonResponseDTO savedWorkDto(Integer lessonId, String email);

    public void saveRecording(Integer lessonId, MultipartFile file, String email);

    public String message(String skill);

    public List<AttemptViewDTO> history(Integer lessonId, String email);

    public List<LessonWorkSubmission> submissionHistory(Integer lessonId, String email);

    public List<LessonWorkSubmissionDTO> submissionHistoryDto(Integer lessonId, String email);

    public List<LessonWorkSubmission> assessableSubmissions(Integer lessonId, String email);

    public List<LessonWorkSubmissionDTO> assessableSubmissionsDto(Integer lessonId, String email);

    public String submissionAudio(Integer lessonId, Integer submissionId, String email);

    public void submitResponse(Integer lessonId, String text, String email);

    public CompletionStateDTO completion(Integer lessonId, String email);

    public void section(Integer lessonId, String section, String email);

    public String lastSection(Integer lessonId, String email);

    public LessonStatsDTO stats(Integer lessonId);

    public String response(Integer lessonId, String email);

    public void saveResponse(Integer lessonId, String text, String email);

    public String assess(Integer lessonId, Integer submissionId, String email);

    public List<CheckViewDTO> questions(Integer id);

    public List<CheckViewDTO> questions(Integer id, String email);

    public List<LessonCheckDTO> editorQuestions(Integer id);

    public void validate(LessonEditorForm f);

    public void saveChecks(Integer id, List<LessonCheckDTO> forms);

    public FeedbackDTO grade(Integer id, Map<String, String> answers, String email);

    public LessonMediaResourceDTO getLessonAudio(Integer lessonId, String email);

    public String getStudentRecordingPath(Integer lessonId, String email);

    public String getLessonImagePath(Integer lessonId, String email);

    public String getLessonVideoPath(Integer lessonId, String email);

    public String videoId(String videoUrl);
}
