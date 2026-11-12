package com.aptisprep.mvc;

import com.aptisprep.mvc.service.exam.AttemptOutboxService;
import com.aptisprep.mvc.service.grading.GeminiGradingService;

import static org.junit.jupiter.api.Assertions.assertSame;

import com.aptisprep.mvc.service.exam.impl.AttemptOutboxServiceImpl;
import com.aptisprep.mvc.service.grading.impl.GeminiGradingServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest
class AptisPrepApplicationTests {
    @Autowired ApplicationContext context;

    @Test
    void contextLoads() {}

    @Test
    void businessServicesUseTheSharedConfiguredJsonMapper() {
        JsonMapper shared = context.getBean(JsonMapper.class);
        for (Class<?> service :
                new Class<?>[] {AttemptOutboxService.class, GeminiGradingService.class}) {
            assertSame(shared, ReflectionTestUtils.getField(context.getBean(service), "json"));
        }
    }
}










