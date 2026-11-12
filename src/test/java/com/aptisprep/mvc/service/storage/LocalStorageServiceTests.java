package com.aptisprep.mvc.service.storage;

import static org.junit.jupiter.api.Assertions.*;

import com.aptisprep.mvc.config.StorageProperties;
import com.aptisprep.mvc.exception.BusinessRuleException;
import com.aptisprep.mvc.service.storage.impl.AudioDurationServiceImpl;
import com.aptisprep.mvc.service.storage.impl.StorageServiceImpl;
import com.aptisprep.mvc.support.TestAudio;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

class LocalStorageServiceTests {
    @TempDir Path root;

    private StorageService storage() {
        var properties = new StorageProperties();
        properties.setUploadDir(root);
        return new StorageServiceImpl(properties);
    }

    @Test
    void storesSafeNameAndLoadsBytes() throws Exception {
        var storage = storage();
        String name =
                storage.store(
                        new MockMultipartFile(
                                "file", "image.PNG", "image/png", new byte[] {1, 2, 3}),
                        "questions");
        assertTrue(name.startsWith("questions/"));
        assertTrue(name.endsWith(".png"));
        assertArrayEquals(new byte[] {1, 2, 3}, storage.load(name).getContentAsByteArray());
        assertTrue(Files.exists(root.resolve(name)));
    }

    @Test
    void storesGifAcceptedByTheLessonEditor() throws Exception {
        StorageService storage = storage();
        byte[] image =
                Base64.getDecoder().decode("R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==");
        String relativePath =
                storage.store(
                        new MockMultipartFile("image", "illustration.gif", "image/gif", image),
                        "lesson-images/1");
        assertTrue(relativePath.endsWith(".gif"));
        assertArrayEquals(image, storage.load(relativePath).getContentAsByteArray());
    }

    @Test
    void rejectsActiveContentAndEmptyFiles() {
        for (String filename :
                new String[] {"script.html", "image.svg", "code.js", "no-extension"}) {
            assertThrows(
                    BusinessRuleException.class,
                    () ->
                            storage()
                                    .store(
                                            new MockMultipartFile(
                                                    "file", filename, "text/plain", new byte[] {1}),
                                            "questions"));
        }
        assertThrows(
                BusinessRuleException.class,
                () -> storage().store(new MockMultipartFile("file", new byte[0]), "questions"));
    }

    @Test
    void rejectsPathsOutsideUploadRoot() {
        assertThrows(
                BusinessRuleException.class,
                () ->
                        storage()
                                .store(
                                        new MockMultipartFile(
                                                "file", "a.png", "image/png", new byte[] {1}),
                                        "../outside"));
        assertThrows(BusinessRuleException.class, () -> storage().load("../outside.wav"));
        assertThrows(BusinessRuleException.class, () -> storage().delete("../outside.wav"));
    }

    @Test
    void legacyCurriculumPathsReadFromUploadRootWithoutChangingStoredReferences() throws Exception {
        var storage = storage();
        String relativePath = "curriculum/audio/listening-a2.wav";
        Path audio = root.resolve(relativePath);
        Files.createDirectories(audio.getParent());
        byte[] expected = TestAudio.wav();
        Files.write(audio, expected);

        assertEquals(audio.toFile(), storage.load(relativePath).getFile());
        assertArrayEquals(expected, storage.load(relativePath).getContentAsByteArray());
        assertEquals(
                1.0,
                new AudioDurationServiceImpl(storage, "missing-ffprobe-for-test")
                        .seconds(relativePath),
                0.01);

        storage.delete(relativePath);
        assertFalse(Files.exists(audio));
        assertThrows(BusinessRuleException.class, () -> storage.load(relativePath));
    }
}
