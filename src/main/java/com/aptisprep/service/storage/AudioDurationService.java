package com.aptisprep.service.storage;

import com.aptisprep.exception.BusinessRuleException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.jaudiotagger.audio.AudioFileIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/** Duration comes from server-side media, never from a learner's request. */
@Service
public class AudioDurationService {
    private final StorageService storage;
    private final String ffprobe;

    public AudioDurationService(
            StorageService storage, @Value("${app.media.ffprobe-path:ffprobe}") String ffprobe) {
        this.storage = storage;
        this.ffprobe = ffprobe;
    }

    public double seconds(String relativePath) {
        var resource = storage.load(relativePath);
        Path temporary = null;
        try {
            Path path;
            try {
                path = resource.getFile().toPath();
            } catch (IOException bundledInsideJar) {
                String extension =
                        relativePath
                                .substring(relativePath.lastIndexOf('.'))
                                .toLowerCase(Locale.ROOT);
                if (!extension.matches("\\.[a-z0-9]{2,5}")) extension = ".audio";
                temporary = Files.createTempFile("aptis-audio-metadata-", extension);
                try (var input = resource.getInputStream()) {
                    Files.copy(input, temporary, StandardCopyOption.REPLACE_EXISTING);
                }
                path = temporary;
            }
            return read(path);
        } catch (IOException unreadable) {
            throw new BusinessRuleException("Không đọc được file audio trên máy chủ");
        } finally {
            if (temporary != null)
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException cleanupFailed) {
                    temporary.toFile().deleteOnExit();
                }
        }
    }

    private double read(Path path) {
        try {
            double seconds =
                    AudioFileIO.read(path.toFile()).getAudioHeader().getPreciseTrackLength();
            if (valid(seconds)) return seconds;
        } catch (Exception unsupportedFormat) {
            // WEBM and files without supported headers can use a configured ffprobe.
        }
        Process process = null;
        try {
            process =
                    new ProcessBuilder(
                                    ffprobe,
                                    "-v",
                                    "error",
                                    "-show_entries",
                                    "format=duration",
                                    "-of",
                                    "default=noprint_wrappers=1:nokey=1",
                                    path.toAbsolutePath().toString())
                            .redirectError(ProcessBuilder.Redirect.DISCARD)
                            .start();
            if (process.waitFor(10, TimeUnit.SECONDS) && process.exitValue() == 0) {
                double seconds =
                        Double.parseDouble(
                                new String(
                                                process.getInputStream().readNBytes(128),
                                                StandardCharsets.UTF_8)
                                        .trim());
                if (valid(seconds)) return seconds;
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception unavailable) {
            // Fail closed: an unverified client duration must not unlock completion.
        } finally {
            if (process != null && process.isAlive()) process.destroyForcibly();
        }
        throw new BusinessRuleException(
                "Không đọc được thời lượng audio. Hãy nhờ quản trị viên kiểm tra file hoặc cấu hình"
                        + " ffprobe.");
    }

    private boolean valid(double seconds) {
        return Double.isFinite(seconds) && seconds > 0 && seconds <= 86400;
    }
}
