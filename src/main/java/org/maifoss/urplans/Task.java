package org.maifoss.urplans;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    private String date;
    private String description;

    @Enumerated(EnumType.STRING)
    private Priority priority;

    public enum Priority {
        URGENT_IMPORTANT,
        NOT_URGENT_IMPORTANT,
        URGENT_NOT_IMPORTANT,
        NOT_URGENT_NOT_IMPORTANT
    }
}
