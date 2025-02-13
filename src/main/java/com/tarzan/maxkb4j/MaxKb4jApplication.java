package com.tarzan.maxkb4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
//@EnableCaching
public class MaxKb4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaxKb4jApplication.class, args);
    }

}
