package com.tarzan.maxkb4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableCaching
@SpringBootApplication(exclude = ThymeleafAutoConfiguration.class)
public class MaxKb4jApplication  {

    public static void main(String[] args) {
        SpringApplication.run(MaxKb4jApplication.class, args);
    }

}
