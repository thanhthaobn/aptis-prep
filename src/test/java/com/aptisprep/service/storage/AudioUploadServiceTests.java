package com.aptisprep.service.storage;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aptisprep.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AudioUploadServiceTests {
    private final StorageService storage = mock(StorageService.class);
    private final AudioUploadService service = new AudioUploadService(storage);

    @Test
    void rejectsTextDisguisedAsAudio() {
        var file = new MockMultipartFile("audioFile", "answer.mp3", "audio/mpeg",
                "not an audio file".getBytes(StandardCharsets.UTF_8));

        assertThrows(BusinessRuleException.class, () -> service.store(file, 10));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsOversizedAudioBeforeStorage() {
        var bytes = new byte[25 * 1024 * 1024 + 1];
        var file = new MockMultipartFile("audioFile", "answer.wav", "audio/wav", bytes);

        assertThrows(BusinessRuleException.class, () -> service.store(file, 10));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsAudioWithMissingMimeTypeEvenWhenExtensionLooksValid() {
        byte[] bytes = new byte[16];
        bytes[0] = 'R'; bytes[1] = 'I'; bytes[2] = 'F'; bytes[3] = 'F';
        bytes[8] = 'W'; bytes[9] = 'A'; bytes[10] = 'V'; bytes[11] = 'E';
        var file = new MockMultipartFile("audioFile", "answer.wav", "", bytes);

        assertThrows(BusinessRuleException.class, () -> service.store(file, 10));
        verifyNoInteractions(storage);
    }

    @Test
    void acceptsAudioWithValidContainerSignatureAndStoresInAttemptScope() {
        byte[] bytes = new byte[16];
        bytes[0] = 'R'; bytes[1] = 'I'; bytes[2] = 'F'; bytes[3] = 'F';
        bytes[8] = 'W'; bytes[9] = 'A'; bytes[10] = 'V'; bytes[11] = 'E';
        var file = new MockMultipartFile("audioFile", "answer.wav", "audio/wav", bytes);
        org.mockito.Mockito.when(storage.store(file, "attempts/10/audio")).thenReturn("attempts/10/audio/answer.wav");

        assertEquals("attempts/10/audio/answer.wav", service.store(file, 10));
        verify(storage).store(file, "attempts/10/audio");
    }
}
