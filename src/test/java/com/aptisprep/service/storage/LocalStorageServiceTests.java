package com.aptisprep.service.storage;

import com.aptisprep.config.StorageProperties;
import com.aptisprep.exception.BusinessRuleException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTests {
    @TempDir Path root;
    private LocalStorageService storage() {
        var properties = new StorageProperties(); properties.setUploadDir(root);
        return new LocalStorageService(properties);
    }
    @Test void storesSafeNameAndLoadsBytes() throws Exception {
        var storage = storage();
        String name = storage.store(new MockMultipartFile("file","image.PNG","image/png",new byte[]{1,2,3}),"questions");
        assertTrue(name.startsWith("questions/")); assertTrue(name.endsWith(".png"));
        assertArrayEquals(new byte[]{1,2,3}, storage.load(name).getContentAsByteArray());
        assertTrue(Files.exists(root.resolve(name)));
    }
    @Test void rejectsActiveContentAndEmptyFiles() {
        for(String filename : new String[]{"script.html","image.svg","code.js","no-extension"}) {
            assertThrows(BusinessRuleException.class, () -> storage().store(new MockMultipartFile("file",filename,"text/plain",new byte[]{1}),"questions"));
        }
        assertThrows(BusinessRuleException.class, () -> storage().store(new MockMultipartFile("file",new byte[0]),"questions"));
    }
    @Test void rejectsPathsOutsideUploadRoot() {
        assertThrows(BusinessRuleException.class, () -> storage().store(new MockMultipartFile("file","a.png","image/png",new byte[]{1}),"../outside"));
        assertThrows(BusinessRuleException.class, () -> storage().load("../outside.wav"));
        assertThrows(BusinessRuleException.class, () -> storage().delete("../outside.wav"));
    }
}
