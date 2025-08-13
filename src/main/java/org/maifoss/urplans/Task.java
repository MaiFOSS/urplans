package org.maifoss.urplans;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 2000)
    private String description;

    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    public enum Priority {
        URGENT_IMPORTANT,
        NOT_URGENT_IMPORTANT,
        URGENT_NOT_IMPORTANT,
        NOT_URGENT_NOT_IMPORTANT
    }
}
