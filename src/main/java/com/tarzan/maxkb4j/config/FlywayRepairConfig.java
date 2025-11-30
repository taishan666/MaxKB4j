package com.tarzan.maxkb4j.config;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev") // 仅在 dev 环境生效，避免误用于生产
public class FlywayRepairConfig {

    @Bean
    public CommandLineRunner flywayRepair(Flyway flyway) {
        return args -> {
            System.out.println("🔧 执行 Flyway repair...");
            flyway.repair();
            System.out.println("✅ Flyway repair 完成");
        };
    }
}