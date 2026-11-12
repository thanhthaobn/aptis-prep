package com.aptisprep.mvc.support;

import com.aptisprep.mvc.service.storage.StorageService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import org.springframework.mock.web.MockMultipartFile;

/** Small generated fixture; tests never depend on or modify the user's media. */
public final class TestAudio {
    private TestAudio() {}

    public static byte[] wav() {
        var format = new AudioFormat(8000, 16, 1, true, false);
        try (var input =
                        new AudioInputStream(
                                new ByteArrayInputStream(new byte[16000]), format, 8000);
                var output = new ByteArrayOutputStream()) {
            AudioSystem.write(input, AudioFileFormat.Type.WAVE, output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot generate test audio", exception);
        }
    }

    public static String store(StorageService storage) {
        return storage.store(
                new MockMultipartFile("audio", "test.wav", "audio/wav", wav()), "test-audio");
    }
}










