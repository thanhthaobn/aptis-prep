package com.aptisprep.service.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.aptisprep.entity.QuestionMediaEntity;
import com.aptisprep.exception.BusinessRuleException;
import com.aptisprep.repository.QuestionMediaRepository;
import com.aptisprep.service.storage.StorageService;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class QuestionMediaWriterTests {
    @Test
    void rejectsInvalidTypeAndSignatureWithoutStoringAnything() {
        var repository = mock(QuestionMediaRepository.class);
        var storage = mock(StorageService.class);
        var writer = new QuestionMediaWriter(repository, storage);
        assertThrows(
                BusinessRuleException.class,
                () ->
                        writer.save(
                                1,
                                new MockMultipartFile(
                                        "file", "script.html", "text/html", "<html>".getBytes())));
        assertThrows(
                BusinessRuleException.class,
                () ->
                        writer.save(
                                1,
                                new MockMultipartFile(
                                        "file", "fake.wav", "audio/wav", "not audio".getBytes())));
        verifyNoInteractions(repository, storage);
    }

    @Test
    void acceptsWebmAudioFromBrowserRecording() {
        var repository = mock(QuestionMediaRepository.class);
        var storage = mock(StorageService.class);
        var file =
                new MockMultipartFile(
                        "file",
                        "clip.webm",
                        "audio/webm",
                        new byte[] {0x1A, 0x45, (byte) 0xDF, (byte) 0xA3, 0, 0, 0, 0});
        when(storage.store(file, "questions/")).thenReturn("questions/clip.webm");
        new QuestionMediaWriter(repository, storage).save(7, file);
        verify(repository)
                .save(
                        argThat(
                                media ->
                                        "AUDIO".equals(media.getMediaType())
                                                && "audio/webm".equals(media.getContentType())
                                                && "questions/clip.webm"
                                                        .equals(media.getMediaUrl())));
    }

    @Test
    void retainsAudioPolicyAndSanitizesUploadedFilename() {
        var repository = mock(QuestionMediaRepository.class);
        var storage = mock(StorageService.class);
        var file =
                new MockMultipartFile(
                        "file",
                        "C:\\uploads\\answer.wav",
                        "audio/wav",
                        new byte[] {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'A', 'V', 'E'});
        when(storage.store(file, "questions/")).thenReturn("questions/saved.wav");
        new QuestionMediaWriter(repository, storage).save(7, file);
        var saved = ArgumentCaptor.forClass(QuestionMediaEntity.class);
        verify(repository).save(saved.capture());
        assertEquals(7, saved.getValue().getQuestionId());
        assertEquals("answer.wav", saved.getValue().getOriginalFilename());
        assertEquals("AUDIO", saved.getValue().getMediaType());
        assertEquals(2, saved.getValue().getMaxPlayCount());
        assertEquals("questions/saved.wav", saved.getValue().getMediaUrl());
    }
}
