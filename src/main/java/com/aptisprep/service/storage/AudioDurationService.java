package com.aptisprep.service.storage;

import com.aptisprep.exception.BusinessRuleException;

import org.jaudiotagger.audio.AudioFileIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

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
            } catch (java.io.IOException bundledInsideJar) {
                String extension =
                        relativePath
                                .substring(relativePath.lastIndexOf('.'))
                                .toLowerCase(java.util.Locale.ROOT);
                if (!extension.matches("\\.[a-z0-9]{2,5}")) extension = ".audio";
                temporary = java.nio.file.Files.createTempFile("aptis-audio-metadata-", extension);
                try (var input = resource.getInputStream()) {
                    java.nio.file.Files.copy(
                            input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }
                path = temporary;
            }
            return read(path);
        } catch (java.io.IOException unreadable) {
            throw new BusinessRuleException("Không đọc được file audio trên máy chủ");
        } finally {
            if (temporary != null)
                try {
                    java.nio.file.Files.deleteIfExists(temporary);
                } catch (java.io.IOException cleanupFailed) {
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
                                                java.nio.charset.StandardCharsets.UTF_8)
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
