package org.maifoss.urplans.service;

import org.maifoss.urplans.Task;
import org.maifoss.urplans.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class TaskService {

    private final TaskRepository repo;

    public TaskService(TaskRepository repo) {
        this.repo = repo;
    }

    public Task save(Task t) {
        return repo.save(t);
    }

    public List<Task> findAll() {
        return repo.findAll();
    }

    public Optional<Task> findById(Long id) {
        return repo.findById(id);
    }

    public boolean deleteById(Long id) {
        if (!repo.existsById(id)) return false;
        repo.deleteById(id);
        return true;
    }

    public long deleteByDate(LocalDate date) {
        return repo.deleteByDate(date);
    }

    public List<Task> findByDate(LocalDate date) {
        return repo.findByDate(date);
    }

    public List<Task> findByTitle(String title) {
        return repo.findByTitleContainingIgnoreCase(title == null ? "" : title);
    }

    /**
     * Lightweight search used by CLI/controller.
     * Keeps signature permissive: search(title, dateStr, priority, page, size).
     */
    public List<Task> search(String title, String dateStr, Task.Priority priority, int page, int size) {
        LocalDate date = null;
        try {
            if (dateStr != null && !dateStr.isBlank()) {
                date = LocalDate.parse(dateStr);
            }
        } catch (Exception ignored) {}

        final LocalDate finalDate = date;
        int safeSize = Math.max(1, size);
        long skip = (long) Math.max(0, page) * safeSize;

        return repo.findAll().stream()
                .filter(t -> title == null || title.isBlank() ||
                        (t.getTitle() != null && t.getTitle().toLowerCase().contains(title.toLowerCase())))
                .filter(t -> finalDate == null || (t.getDate() != null && t.getDate().equals(finalDate)))
                .filter(t -> priority == null || t.getPriority() == priority)
                .skip(skip)
                .limit(safeSize)
                .collect(Collectors.toList());
    }
}
