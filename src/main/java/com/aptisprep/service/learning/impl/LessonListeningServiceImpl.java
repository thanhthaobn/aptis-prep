package com.aptisprep.service.learning.impl;

import com.aptisprep.entity.Lesson;
import com.aptisprep.entity.LessonEngagement;
import com.aptisprep.entity.User;
import com.aptisprep.mvc.common.enums.Role;
import com.aptisprep.mvc.common.enums.UserStatus;
import com.aptisprep.mvc.common.exception.AccessDeniedException;
import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.common.exception.ResourceNotFoundException;
import com.aptisprep.repository.LessonEngagementRepository;
import com.aptisprep.repository.LessonRepository;
import com.aptisprep.repository.UserRepository;
import com.aptisprep.service.learning.LessonListeningService;
import com.aptisprep.service.learning.LessonListeningService.Playback;
import com.aptisprep.service.storage.AudioDurationService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Server-timed, owner-bound playback. Never credits a range twice or trusts client duration. */
@Service
@Transactional
public class LessonListeningServiceImpl implements LessonListeningService {
    private final UserRepository userRepository;
    private final LessonRepository lessonRepository;
    private final LessonEngagementRepository lessonEngagementRepository;
    private final AudioDurationService durations;
    private final Clock clock;

    @Autowired
    public LessonListeningServiceImpl(
            UserRepository userRepository,
            LessonRepository lessonRepository,
            LessonEngagementRepository lessonEngagementRepository,
            AudioDurationService durations) {
        this(
                userRepository,
                lessonRepository,
                lessonEngagementRepository,
                durations,
                Clock.systemDefaultZone());
    }

    public LessonListeningServiceImpl(
            UserRepository userRepository,
            LessonRepository lessonRepository,
            LessonEngagementRepository lessonEngagementRepository,
            AudioDurationService durations,
            Clock clock) {
        this.userRepository = userRepository;
        this.lessonRepository = lessonRepository;
        this.lessonEngagementRepository = lessonEngagementRepository;
        this.durations = durations;
        this.clock = clock;
    }

    public Playback start(Integer lessonId, String email, double position) {
        User user = activeLearner(email);
        Lesson lesson = publishedAudio(lessonId);
        LessonEngagement value =
                lessonEngagementRepository
                        .findByUserIdAndLessonId(user.getId(), lessonId)
                        .orElseGet(LessonEngagement::new);
        value.setUserId(user.getId());
        value.setLessonId(lessonId);
        double duration = durations.seconds(lesson.getAudioPath());
        validatePosition(position, duration);
        position = Math.min(position, duration);
        if (!Objects.equals(value.getPlaybackAudioPath(), lesson.getAudioPath())) {
            value.replacePlaybackRanges(List.of());
        }
        String token = UUID.randomUUID().toString();
        value.setPlaybackTokenHash(hash(token));
        value.setPlaybackAudioPath(lesson.getAudioPath());
        value.setPlaybackDurationSeconds(duration);
        value.setPlaybackPositionSeconds(position);
        value.setPlaybackLastPingAt(now());
        value.setPlaybackExpiresAt(now().plusMinutes(5));
        value.setPlaybackSequence(0);
        value.setPlaybackRunning(true);
        lessonEngagementRepository.save(value);
        return view(value, token);
    }

    public Playback heartbeat(
            Integer lessonId,
            String email,
            String token,
            int sequence,
            double position,
            boolean playing,
            boolean discontinuity) {
        User user = activeLearner(email);
        Lesson lesson = publishedAudio(lessonId);
        LessonEngagement value =
                lessonEngagementRepository
                        .findByUserIdAndLessonId(user.getId(), lessonId)
                        .orElseThrow(
                                () -> new BusinessRuleException("Hãy bắt đầu phiên nghe trước"));
        if (token == null
                || token.length() > 80
                || !Objects.equals(value.getPlaybackTokenHash(), hash(token))
                || value.getPlaybackExpiresAt() == null
                || !now().isBefore(value.getPlaybackExpiresAt())
                || !Objects.equals(value.getPlaybackAudioPath(), lesson.getAudioPath()))
            throw new BusinessRuleException(
                    "Phiên nghe hết hạn hoặc audio đã thay đổi. Hãy phát lại audio.");
        double duration = value.getPlaybackDurationSeconds();
        validatePosition(position, duration);
        position = Math.min(position, duration);
        if (sequence == value.getPlaybackSequence()
                && Double.compare(position, value.getPlaybackPositionSeconds()) == 0)
            return view(value, null);
        if (sequence != value.getPlaybackSequence() + 1)
            throw new BusinessRuleException("Tiến trình nghe không đúng thứ tự");

        double elapsed =
                Math.max(
                        0,
                        Duration.between(value.getPlaybackLastPingAt(), now()).toNanos()
                                / 1_000_000_000.0);
        double advance = position - value.getPlaybackPositionSeconds();
        List<double[]> ranges = ranges(value);
        // Large jumps are seeks, not listening. Credit can never exceed elapsed server time.
        if (value.isPlaybackRunning()
                && !discontinuity
                && advance > 0
                && advance <= Math.min(elapsed, 15) * 1.25 + 1) {
            double credit = Math.min(advance, Math.min(elapsed, 15));
            if (credit > 0)
                ranges.add(
                        new double[] {
                            value.getPlaybackPositionSeconds(),
                            value.getPlaybackPositionSeconds() + credit
                        });
        }
        ranges = merge(ranges);
        value.replacePlaybackRanges(ranges);
        value.setPlaybackPositionSeconds(position);
        value.setPlaybackLastPingAt(now());
        value.setPlaybackExpiresAt(now().plusMinutes(5));
        value.setPlaybackSequence(sequence);
        value.setPlaybackRunning(playing);
        if (length(ranges) >= duration * .9) {
            value.setAudioPlayedAt(now());
            value.setVerifiedAudioPath(lesson.getAudioPath());
        }
        lessonEngagementRepository.save(value);
        return view(value, null);
    }

    public static boolean verified(Lesson lesson, LessonEngagement value) {
        return lesson.getAudioPath() != null
                && value != null
                && value.getAudioPlayedAt() != null
                && Objects.equals(lesson.getAudioPath(), value.getVerifiedAudioPath());
    }

    private Playback view(LessonEngagement value, String token) {
        return new Playback(
                token,
                value.getPlaybackSequence(),
                value.getPlaybackDurationSeconds(),
                length(ranges(value)),
                value.getAudioPlayedAt() != null
                        && Objects.equals(
                                value.getPlaybackAudioPath(), value.getVerifiedAudioPath()));
    }

    private List<double[]> ranges(LessonEngagement value) {
        ArrayList<double[]> result = new ArrayList<>();
        for (var range : value.getPlaybackRanges())
            result.add(new double[] {range.getStartSeconds(), range.getEndSeconds()});
        return result;
    }

    private static List<double[]> merge(List<double[]> ranges) {
        ranges.sort(Comparator.comparingDouble(range -> range[0]));
        ArrayList<double[]> result = new ArrayList<double[]>();
        for (double[] range : ranges) {
            if (result.isEmpty() || range[0] > result.getLast()[1]) result.add(range.clone());
            else result.getLast()[1] = Math.max(result.getLast()[1], range[1]);
        }
        return result;
    }

    private static double length(List<double[]> ranges) {
        return ranges.stream().mapToDouble(range -> range[1] - range[0]).sum();
    }

    private void validatePosition(double position, double duration) {
        // Decoders may round the final sample differently; never credit beyond the server duration.
        if (!Double.isFinite(position) || position < 0 || position > duration + .25)
            throw new BusinessRuleException("Vị trí audio không hợp lệ");
    }

    private User activeLearner(String email) {
        User found =
                userRepository
                        .findByEmailIgnoreCase(email)
                        .orElseThrow(() -> new AccessDeniedException("Không tìm thấy tài khoản"));
        User user = userRepository.findByIdForUpdate(found.getId()).orElseThrow();
        if (user.getRole() != Role.STUDENT || user.getStatus() != UserStatus.ACTIVE)
            throw new AccessDeniedException("Tài khoản không được phép học");
        return user;
    }

    private Lesson publishedAudio(Integer id) {
        return lessonRepository
                .findById(id)
                .filter(
                        lesson ->
                                "PUBLISHED".equals(lesson.getStatus())
                                        && lesson.getAudioPath() != null)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Bài học chưa có audio được công bố"));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    private static String hash(String token) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
