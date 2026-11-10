package com.aptisprep;

import com.aptisprep.mvc.common.config.AiProperties;
import com.aptisprep.mvc.common.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {"com.aptisprep", "com.aptisprep.mvc"})
@EnableScheduling
@EnableConfigurationProperties({StorageProperties.class, AiProperties.class})
public class AptisPrepApplication {

    public static void main(String[] args) {
        SpringApplication.run(AptisPrepApplication.class, args);
    }
}
