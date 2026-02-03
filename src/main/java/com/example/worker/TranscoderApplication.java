package com.example.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class TranscoderApplication {

    public static void main(String[] args) {
        SpringApplication.run(TranscoderApplication.class, args);
    }

}
