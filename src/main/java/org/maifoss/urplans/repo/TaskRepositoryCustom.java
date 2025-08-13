// File: src/main/java/org/maifoss/urplans/repo/TaskRepositoryCustom.java
package org.maifoss.urplans.repo;

import org.maifoss.urplans.Task;
import org.maifoss.urplans.Task.Priority;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TaskRepositoryCustom {
    List<Task> search(Optional<String> title,
                      Optional<LocalDate> date,
                      Optional<Priority> priority,
                      int offset,
                      int limit);
}