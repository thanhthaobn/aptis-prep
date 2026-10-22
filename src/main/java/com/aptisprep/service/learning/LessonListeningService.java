package com.aptisprep.service.learning;

import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonEngagement;
import com.aptisprep.enums.Role;
import com.aptisprep.enums.UserStatus;
import com.aptisprep.exception.AccessDeniedException;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.exception.ResourceNotFoundException;
import com.aptisprep.repository.LessonEngagementRepository;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.storage.AudioDurationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Server-timed, owner-bound playback. Never credits a range twice or trusts client duration. */
@Service
@Transactional
public class LessonListeningService {
    private final UserRepository users;
    private final LessonRepository lessons;
    private final LessonEngagementRepository engagements;
    private final AudioDurationService durations;
    private final Clock clock;
    private final JsonMapper json = JsonMapper.builder().build();

    @Autowired
    public LessonListeningService(UserRepository users, LessonRepository lessons,
            LessonEngagementRepository engagements, AudioDurationService durations) {
        this(users, lessons, engagements, durations, Clock.systemDefaultZone());
    }

    LessonListeningService(UserRepository users, LessonRepository lessons,
            LessonEngagementRepository engagements, AudioDurationService durations, Clock clock) {
        this.users = users; this.lessons = lessons; this.engagements = engagements;
        this.durations = durations; this.clock = clock;
    }

    public record Playback(String token, int sequence, double duration, double listened, boolean verified) {}

    public Playback start(Integer lessonId, String email, double position) {
        var user = activeLearner(email);
        var lesson = publishedAudio(lessonId);
        var value = engagements.findByUserIdAndLessonId(user.getId(), lessonId).orElseGet(LessonEngagement::new);
        value.setUserId(user.getId()); value.setLessonId(lessonId);
        double duration = durations.seconds(lesson.getAudioPath());
        validatePosition(position, duration);
        position = Math.min(position, duration);
        if (!Objects.equals(value.getPlaybackAudioPath(), lesson.getAudioPath())) {
            value.setPlaybackRangesJson("[]");
        }
        String token = UUID.randomUUID().toString();
        value.setPlaybackTokenHash(hash(token)); value.setPlaybackAudioPath(lesson.getAudioPath());
        value.setPlaybackDurationSeconds(duration); value.setPlaybackPositionSeconds(position);
        value.setPlaybackLastPingAt(now()); value.setPlaybackExpiresAt(now().plusMinutes(5));
        value.setPlaybackSequence(0); value.setPlaybackRunning(true);
        engagements.save(value);
        return view(value, token);
    }

    public Playback heartbeat(Integer lessonId, String email, String token, int sequence,
            double position, boolean playing, boolean discontinuity) {
        var user = activeLearner(email);
        var lesson = publishedAudio(lessonId);
        var value = engagements.findByUserIdAndLessonId(user.getId(), lessonId)
                .orElseThrow(() -> new BusinessRuleException("Hãy bắt đầu phiên nghe trước"));
        if (token == null || token.length() > 80 || !Objects.equals(value.getPlaybackTokenHash(), hash(token))
                || value.getPlaybackExpiresAt() == null || !now().isBefore(value.getPlaybackExpiresAt())
                || !Objects.equals(value.getPlaybackAudioPath(), lesson.getAudioPath()))
            throw new BusinessRuleException("Phiên nghe hết hạn hoặc audio đã thay đổi. Hãy phát lại audio.");
        double duration = value.getPlaybackDurationSeconds();
        validatePosition(position, duration);
        position = Math.min(position, duration);
        if (sequence == value.getPlaybackSequence()
                && Double.compare(position, value.getPlaybackPositionSeconds()) == 0) return view(value, null);
        if (sequence != value.getPlaybackSequence() + 1)
            throw new BusinessRuleException("Tiến trình nghe không đúng thứ tự");

        double elapsed = Math.max(0, Duration.between(value.getPlaybackLastPingAt(), now()).toNanos() / 1_000_000_000.0);
        double advance = position - value.getPlaybackPositionSeconds();
        var ranges = ranges(value);
        // Large jumps are seeks, not listening. Credit can never exceed elapsed server time.
        if (value.isPlaybackRunning() && !discontinuity && advance > 0
                && advance <= Math.min(elapsed, 15) * 1.25 + 1) {
            double credit = Math.min(advance, Math.min(elapsed, 15));
            if (credit > 0) ranges.add(new double[]{value.getPlaybackPositionSeconds(), value.getPlaybackPositionSeconds() + credit});
        }
        ranges = merge(ranges);
        value.setPlaybackRangesJson(json.writeValueAsString(ranges));
        value.setPlaybackPositionSeconds(position); value.setPlaybackLastPingAt(now());
        value.setPlaybackExpiresAt(now().plusMinutes(5)); value.setPlaybackSequence(sequence);
        value.setPlaybackRunning(playing);
        if (length(ranges) >= duration * .9) {
            value.setAudioPlayedAt(now()); value.setVerifiedAudioPath(lesson.getAudioPath());
        }
        engagements.save(value);
        return view(value, null);
    }

    public static boolean verified(Lesson lesson, LessonEngagement value) {
        return lesson.getAudioPath() != null && value != null && value.getAudioPlayedAt() != null
                && Objects.equals(lesson.getAudioPath(), value.getVerifiedAudioPath());
    }

    private Playback view(LessonEngagement value, String token) {
        return new Playback(token, value.getPlaybackSequence(), value.getPlaybackDurationSeconds(),
                length(ranges(value)), value.getAudioPlayedAt() != null
                    && Objects.equals(value.getPlaybackAudioPath(), value.getVerifiedAudioPath()));
    }

    private List<double[]> ranges(LessonEngagement value) {
        var result = new ArrayList<double[]>();
        if (value.getPlaybackRangesJson() != null)
            for (var range : json.readTree(value.getPlaybackRangesJson())) result.add(new double[]{range.get(0).asDouble(),range.get(1).asDouble()});
        return result;
    }

    private static List<double[]> merge(List<double[]> ranges) {
        ranges.sort(Comparator.comparingDouble(range -> range[0]));
        var result = new ArrayList<double[]>();
        for (var range : ranges) {
            if (result.isEmpty() || range[0] > result.getLast()[1]) result.add(range.clone());
            else result.getLast()[1] = Math.max(result.getLast()[1], range[1]);
        }
        return result;
    }

    private static double length(List<double[]> ranges) { return ranges.stream().mapToDouble(range -> range[1]-range[0]).sum(); }
    private void validatePosition(double position, double duration) {
        // Decoders may round the final sample differently; never credit beyond the server duration.
        if (!Double.isFinite(position) || position < 0 || position > duration + .25)
            throw new BusinessRuleException("Vị trí audio không hợp lệ");
    }
    private com.aptisprep.entity.User activeLearner(String email) {
        var found = users.findByEmailIgnoreCase(email).orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản"));
        var user = users.findByIdForUpdate(found.getId()).orElseThrow();
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không được phép học");
        return user;
    }
    private Lesson publishedAudio(Integer id) {
        return lessons.findById(id).filter(lesson -> "PUBLISHED".equals(lesson.getStatus()) && lesson.getAudioPath()!=null)
                .orElseThrow(() -> new ResourceNotFoundException("Bài học chưa có audio được công bố"));
    }
    private LocalDateTime now() { return LocalDateTime.now(clock); }
    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
