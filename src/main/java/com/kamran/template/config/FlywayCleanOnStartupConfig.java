package com.kamran.template.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
        name = "SPRING_FLYWAY_CLEAN_ON_STARTUP",
        havingValue = "true"
)
public class FlywayCleanOnStartupConfig {

    @Bean
    FlywayMigrationStrategy flywayCleanMigrateStrategy() {
        return flyway -> {
            flyway.clean();
            flyway.migrate();
        };
    }
}

