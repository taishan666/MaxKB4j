package com.maxkb4j.start.config;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Slf4j
@Configuration
@Profile("dev") // 仅在 dev 环境生效，避免误用于生产
public class FlywayRepairConfig {

    @Bean
    public CommandLineRunner flywayRepair(Flyway flyway) {
        return args -> {
            log.info("🔧 执行 Flyway repair...");
            flyway.repair();
            log.info("✅ Flyway repair 完成");
        };
    }
}