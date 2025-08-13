package org.maifoss.urplans;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByDate(LocalDate date);

    List<Task> findByTitleContainingIgnoreCase(String title);

    long deleteByDate(LocalDate date);
}
