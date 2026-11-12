package com.aptisprep.mvc;

import com.aptisprep.mvc.config.AiProperties;
import com.aptisprep.mvc.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({StorageProperties.class, AiProperties.class})
public class AptisPrepApplication {

    public static void main(String[] args) {
        SpringApplication.run(AptisPrepApplication.class, args);
    }
}
