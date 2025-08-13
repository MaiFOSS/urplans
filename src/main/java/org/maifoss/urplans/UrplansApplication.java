package org.maifoss.urplans;

import org.maifoss.urplans.service.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;

@SpringBootApplication
public class UrplansApplication {
    public static void main(String[] args) {
        SpringApplication.run(UrplansApplication.class, args);
    }

    @Bean
    CommandLineRunner seed(TaskService svc) {
        return args -> {
            svc.save(new Task(null, "Study Java", "Study to get prepared", LocalDate.of(2025, 8, 13), Task.Priority.URGENT_IMPORTANT));
            svc.save(new Task(null, "Write README", "Document the project", LocalDate.now(), Task.Priority.NOT_URGENT_IMPORTANT));
        };
    }
}
