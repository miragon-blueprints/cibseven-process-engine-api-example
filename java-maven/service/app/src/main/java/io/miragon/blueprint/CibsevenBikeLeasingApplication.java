package io.miragon.blueprint;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class CibsevenBikeLeasingApplication {

    /** Single source of "now" for the app, so time-dependent logic can be pinned in tests. */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    public static void main(String[] args) {
        SpringApplication.run(CibsevenBikeLeasingApplication.class, args);
    }
}
