package com.example.postapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PostApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(PostApiApplication.class, args);
    }
}
