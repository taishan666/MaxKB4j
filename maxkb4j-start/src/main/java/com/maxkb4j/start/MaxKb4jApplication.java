package com.maxkb4j.start;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;


@EnableScheduling
@EnableCaching
@SpringBootApplication(scanBasePackages = {"com.maxkb4j"}, exclude = ThymeleafAutoConfiguration.class)
public class MaxKb4jApplication {
    public static void main(String[] args) {
        // 检查是否设置了 active profile，未设置则默认使用 dev
        if (System.getProperty("spring.profiles.active") == null && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
            System.setProperty("spring.profiles.active", "dev");
        }
        // langchain4j classpath 上存在多个 HTTP client 实现，显式指定使用 Spring RestClient
        System.setProperty("langchain4j.http.clientBuilderFactory", "dev.langchain4j.http.client.spring.restclient.SpringRestClientBuilderFactory");
        SpringApplication.run(MaxKb4jApplication.class, args);
    }

}
