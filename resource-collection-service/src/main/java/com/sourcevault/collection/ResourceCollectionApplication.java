package com.sourcevault.collection;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ResourceCollectionApplication {
    public static void main(String[] args) {
        SpringApplication.run(ResourceCollectionApplication.class, args);
    }
}
