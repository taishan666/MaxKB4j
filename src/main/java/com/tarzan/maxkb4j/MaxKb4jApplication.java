package com.tarzan.maxkb4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MaxKb4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaxKb4jApplication.class, args);
    }

}
