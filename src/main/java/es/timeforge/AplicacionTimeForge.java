package es.timeforge;

import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AplicacionTimeForge {
    public static void main(String[] args) {
        SpringApplication.run(AplicacionTimeForge.class, args);
    }

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }
}
