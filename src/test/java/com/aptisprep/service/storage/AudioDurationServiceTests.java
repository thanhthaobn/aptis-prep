package com.aptisprep.service.storage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.aptisprep.exception.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

class AudioDurationServiceTests {
    @Test void readsDurationFromServerFileRatherThanAClientParameter() {
        var storage=mock(StorageService.class);
        when(storage.load("curriculum/audio/listening-a2.wav"))
                .thenReturn(new ClassPathResource("curriculum/audio/listening-a2.wav"));
        double duration=new AudioDurationService(storage,"missing-ffprobe-for-test").seconds("curriculum/audio/listening-a2.wav");
        assertTrue(duration>0&&duration<86400);
    }
    @Test void bundledJarResourcesCanBeProbedWithoutARealFilesystemFile() throws Exception {
        byte[] bytes;
        try(var input=new ClassPathResource("curriculum/audio/listening-a2.wav").getInputStream()){bytes=input.readAllBytes();}
        var storage=mock(StorageService.class);when(storage.load("inside-jar.wav")).thenReturn(new ByteArrayResource(bytes));
        assertTrue(new AudioDurationService(storage,"missing-ffprobe-for-test").seconds("inside-jar.wav")>0);
    }
    @Test void invalidMediaFailsClosedRatherThanAcceptingAnInventedDuration() {
        var storage=mock(StorageService.class);when(storage.load("invalid.wav")).thenReturn(new ByteArrayResource(new byte[]{1,2,3}));
        assertThrows(BusinessRuleException.class,()->new AudioDurationService(storage,"missing-ffprobe-for-test").seconds("invalid.wav"));
    }
}
