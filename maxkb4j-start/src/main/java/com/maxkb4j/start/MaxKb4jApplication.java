package com.maxkb4j.start;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableCaching
@MapperScan(basePackages = {
    "com.maxkb4j.application.mapper",
    "com.maxkb4j.knowledge.mapper",
    "com.maxkb4j.model.mapper",
    "com.maxkb4j.tool.mapper",
    "com.maxkb4j.system.mapper",
    "com.maxkb4j.oss.mapper"
})
@SpringBootApplication(scanBasePackages = {"com.maxkb4j"},exclude = ThymeleafAutoConfiguration.class)
public class MaxKb4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaxKb4jApplication.class, args);
    }

}
