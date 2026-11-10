package com.aptisprep.mvc.storage.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.mvc.common.exception.BusinessRuleException;
import com.aptisprep.mvc.storage.service.impl.AudioDurationServiceImpl;
import com.aptisprep.mvc.storage.support.TestAudio;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;

class AudioDurationServiceTests {
    @TempDir Path root;

    @Test
    void readsDurationFromServerFileRatherThanAClientParameter() throws Exception {
        var file = Files.write(root.resolve("test.wav"), TestAudio.wav());
        var storage = mock(StorageService.class);
        when(storage.load("audio/test.wav")).thenReturn(new FileSystemResource(file));
        double duration =
                new AudioDurationServiceImpl(storage, "missing-ffprobe-for-test")
                        .seconds("audio/test.wav");
        assertEquals(1.0, duration, 0.01);
    }

    @Test
    void streamResourcesCanBeProbedThroughATemporaryFile() {
        byte[] bytes = TestAudio.wav();
        var storage = mock(StorageService.class);
        when(storage.load("stream.wav")).thenReturn(new ByteArrayResource(bytes));
        assertTrue(
                new AudioDurationServiceImpl(storage, "missing-ffprobe-for-test")
                                .seconds("stream.wav")
                        > 0);
    }

    @Test
    void extensionlessInvalidStreamReturnsABusinessErrorRatherThanAnIndexError() {
        StorageService storage = mock(StorageService.class);
        when(storage.load("audio/legacy-recording"))
                .thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));
        assertThrows(
                BusinessRuleException.class,
                () ->
                        new AudioDurationServiceImpl(storage, "missing-ffprobe-for-test")
                                .seconds("audio/legacy-recording"));
    }

    @Test
    void invalidMediaFailsClosedRatherThanAcceptingAnInventedDuration() {
        var storage = mock(StorageService.class);
        when(storage.load("invalid.wav")).thenReturn(new ByteArrayResource(new byte[] {1, 2, 3}));
        assertThrows(
                BusinessRuleException.class,
                () ->
                        new AudioDurationServiceImpl(storage, "missing-ffprobe-for-test")
                                .seconds("invalid.wav"));
    }
}
