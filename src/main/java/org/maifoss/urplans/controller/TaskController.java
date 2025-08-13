// File: src/main/java/org/maifoss/urplans/controller/TaskController.java
package org.maifoss.urplans.controller;

import org.maifoss.urplans.Task;
import org.maifoss.urplans.Task.Priority;
import org.maifoss.urplans.service.TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskService svc;

    public TaskController(TaskService svc) { this.svc = svc; }

    @GetMapping("/search")
    public List<Task> search(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) Priority priority,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return svc.search(title, date, priority, page, size);
    }
}
