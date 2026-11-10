package com.aptisprep.mvc.storage.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aptisprep.mvc.common.exception.BusinessRuleException;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class AudioUploadServiceTests {
    private final StorageService storage = mock(StorageService.class);
    private final AudioUploadService service =
            new com.aptisprep.mvc.storage.service.impl.AudioUploadServiceImpl(storage);

    @Test
    void rejectsTextDisguisedAsAudio() {
        var file =
                new MockMultipartFile(
                        "audioFile",
                        "answer.mp3",
                        "audio/mpeg",
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
        bytes[0] = 'R';
        bytes[1] = 'I';
        bytes[2] = 'F';
        bytes[3] = 'F';
        bytes[8] = 'W';
        bytes[9] = 'A';
        bytes[10] = 'V';
        bytes[11] = 'E';
        var file = new MockMultipartFile("audioFile", "answer.wav", "", bytes);

        assertThrows(BusinessRuleException.class, () -> service.store(file, 10));
        verifyNoInteractions(storage);
    }

    @Test
    void closesValidationStreamEvenWhenAudioIsRejected() throws Exception {
        var input = org.mockito.Mockito.spy(new java.io.ByteArrayInputStream(new byte[] {1, 2, 3}));
        var file = mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.when(file.getSize()).thenReturn(3L);
        org.mockito.Mockito.when(file.getOriginalFilename()).thenReturn("answer.wav");
        org.mockito.Mockito.when(file.getContentType()).thenReturn("audio/wav");
        org.mockito.Mockito.when(file.getInputStream()).thenReturn(input);

        assertThrows(BusinessRuleException.class, () -> service.store(file, 10));
        verify(input).close();
        verifyNoInteractions(storage);
    }

    @Test
    void acceptsAudioWithValidContainerSignatureAndStoresInAttemptScope() {
        byte[] bytes = new byte[16];
        bytes[0] = 'R';
        bytes[1] = 'I';
        bytes[2] = 'F';
        bytes[3] = 'F';
        bytes[8] = 'W';
        bytes[9] = 'A';
        bytes[10] = 'V';
        bytes[11] = 'E';
        var file = new MockMultipartFile("audioFile", "answer.wav", "audio/wav", bytes);
        org.mockito.Mockito.when(storage.store(file, "attempts/10/audio"))
                .thenReturn("attempts/10/audio/answer.wav");

        assertEquals("attempts/10/audio/answer.wav", service.store(file, 10));
        verify(storage).store(file, "attempts/10/audio");
    }
}
