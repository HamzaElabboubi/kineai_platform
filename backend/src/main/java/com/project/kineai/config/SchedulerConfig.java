package com.project.kineai.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SchedulerConfig {

    // En production, Spring injecte l'horloge système réelle.
    // Les tests créent leur propre Clock.fixed(...) sans passer
    // par ce bean.
    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}