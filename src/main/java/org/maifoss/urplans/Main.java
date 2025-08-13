package org.maifoss.urplans;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import java.util.Base64;
import java.util.stream.Collectors;

public class Main {
    private static final Path DB = Paths.get("tasks.db");
    private static long nextId = 1;

    public static void main(String[] args) throws Exception {
        List<CliTask> tasks = loadTasks();
        if (!tasks.isEmpty()) {
            nextId = tasks.stream().mapToLong(t -> t.id).max().orElse(0L) + 1;
        }

        if (args.length == 0) {
            System.out.println(toJson(tasks));
            return;
        }

        String cmd = args[0];
        switch (cmd) {
            case "--add" -> {
                String input = joinArgs(args, 1);
                if (input.isBlank()) {
                    System.err.println("Usage: --add \"title|description|PRIORITY|[date]\"");
                    return;
                }
                CliTask t = parseInputToTask(input);
                t.id = nextId++;
                tasks.add(t);
                persist(tasks);
                System.out.println(toJson(t));
            }
            case "--list" -> {
                System.out.println(toJson(tasks));
            }
            case "--list-date" -> {
                if (args.length < 2) {
                    System.err.println("Usage: --list-date YYYY-MM-DD");
                    return;
                }
                String date = args[1];
                List<CliTask> found = tasks.stream().filter(t -> t.date.equals(date)).collect(Collectors.toList());
                System.out.println(toJson(found));
            }
            case "--delete-id" -> {
                if (args.length < 2) {
                    System.err.println("Usage: --delete-id ID");
                    return;
                }
                try {
                    long id = Long.parseLong(args[1]);
                    Optional<CliTask> removed = deleteById(tasks, id);
                    if (removed.isPresent()) {
                        persist(tasks);
                        System.out.println(toJson(removed.get()));
                    } else {
                        System.err.println("No task with id " + id);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid id");
                }
            }
            case "--delete-date" -> {
                if (args.length < 2) {
                    System.err.println("Usage: --delete-date YYYY-MM-DD");
                    return;
                }
                String date = args[1];
                List<CliTask> removed = tasks.stream().filter(t -> t.date.equals(date)).collect(Collectors.toList());
                if (removed.isEmpty()) {
                    System.err.println("No tasks found for " + date);
                    return;
                }
                tasks.removeIf(t -> t.date.equals(date));
                persist(tasks);
                System.out.println(toJson(removed));
            }
            case "--edit-id" -> {
                if (args.length < 3) {
                    System.err.println("Usage: --edit-id ID \"title|description|PRIORITY|[date]\"");
                    return;
                }
                try {
                    long id = Long.parseLong(args[1]);
                    String input = joinArgs(args, 2);
                    CliTask newTask = parseInputToTask(input);
                    Optional<CliTask> edited = editById(tasks, id, newTask);
                    if (edited.isPresent()) {
                        persist(tasks);
                        System.out.println(toJson(edited.get()));
                    } else {
                        System.err.println("No task with id " + id);
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid id");
                }
            }
            case "--help", "-h" -> printHelp();
            default -> {
                System.err.println("Unknown command. Use --help");
            }
        }
    }

    private static String joinArgs(String[] args, int start) {
        if (args.length <= start) return "";
        return String.join(" ", Arrays.copyOfRange(args, start, args.length)).trim();
    }

    private static CliTask parseInputToTask(String input) {
        String[] parts = input.split("\\|", 4);
        String title = parts.length > 0 ? parts[0] : "";
        String description = parts.length > 1 ? parts[1] : "";
        String priorityStr = parts.length > 2 ? parts[2].toUpperCase() : "NOT_URGENT_NOT_IMPORTANT";
        String date = parts.length > 3 && !parts[3].isBlank() ? parts[3] : LocalDate.now().toString();
        CliTask.Priority priority;
        try {
            priority = CliTask.Priority.valueOf(priorityStr);
        } catch (IllegalArgumentException ex) {
            priority = CliTask.Priority.NOT_URGENT_NOT_IMPORTANT;
        }
        return new CliTask(0L, title, date, description, priority);
    }

    private static Optional<CliTask> deleteById(List<CliTask> tasks, long id) {
        for (Iterator<CliTask> it = tasks.iterator(); it.hasNext(); ) {
            CliTask t = it.next();
            if (t.id == id) {
                it.remove();
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    private static Optional<CliTask> editById(List<CliTask> tasks, long id, CliTask newTask) {
        for (int i = 0; i < tasks.size(); i++) {
            CliTask t = tasks.get(i);
            if (t.id == id) {
                CliTask updated = new CliTask(id,
                        newTask.title.isEmpty() ? t.title : newTask.title,
                        newTask.date.isEmpty() ? t.date : newTask.date,
                        newTask.description.isEmpty() ? t.description : newTask.description,
                        newTask.priority == null ? t.priority : newTask.priority);
                tasks.set(i, updated);
                return Optional.of(updated);
            }
        }
        return Optional.empty();
    }

    private static void printHelp() {
        System.out.println("Usage:");
        System.out.println("  --add \"title|description|PRIORITY|[date]\"   Add a task (date optional, defaults to today)");
        System.out.println("  --list                                       List all tasks (JSON)");
        System.out.println("  --list-date YYYY-MM-DD                       List tasks for a date");
        System.out.println("  --delete-id ID                               Delete a task by id and print it");
        System.out.println("  --delete-date YYYY-MM-DD                     Delete all tasks for a date and print removed");
        System.out.println("  --edit-id ID \"title|description|PRIORITY|[date]\"  Edit task (empty fields keep old values)");
        System.out.println("  --help, -h                                   Show this help");
        System.out.println("Priority values: URGENT_IMPORTANT, NOT_URGENT_IMPORTANT, URGENT_NOT_IMPORTANT, NOT_URGENT_NOT_IMPORTANT");
    }

    private static String toJson(List<CliTask> tasks) {
        return tasks.stream()
                .map(Main::toJson)
                .collect(Collectors.joining(",", "[", "]"));
    }

    private static String toJson(CliTask t) {
        return String.format("{\"id\":%d,\"title\":\"%s\",\"date\":\"%s\",\"desc\":\"%s\",\"priority\":\"%s\"}",
                t.id, escapeJson(t.title), escapeJson(t.date), escapeJson(t.description), t.priority);
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static List<CliTask> loadTasks() {
        if (Files.notExists(DB)) return new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(DB, StandardCharsets.UTF_8);
            List<CliTask> out = new ArrayList<>();
            for (String L : lines) {
                if (L.isBlank()) continue;
                String[] parts = L.split("\\|", 5);
                if (parts.length < 5) continue;
                long id = Long.parseLong(parts[0]);
                String title = new String(Base64.getDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                String date = new String(Base64.getDecoder().decode(parts[2]), StandardCharsets.UTF_8);
                String desc = new String(Base64.getDecoder().decode(parts[3]), StandardCharsets.UTF_8);
                CliTask.Priority pr = CliTask.Priority.valueOf(parts[4]);
                out.add(new CliTask(id, title, date, desc, pr));
            }
            return out;
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }

    private static void persist(List<CliTask> tasks) {
        List<String> lines = tasks.stream().map(t -> {
            String bTitle = Base64.getEncoder().encodeToString(t.title.getBytes(StandardCharsets.UTF_8));
            String bDate = Base64.getEncoder().encodeToString(t.date.getBytes(StandardCharsets.UTF_8));
            String bDesc = Base64.getEncoder().encodeToString(t.description.getBytes(StandardCharsets.UTF_8));
            return t.id + "|" + bTitle + "|" + bDate + "|" + bDesc + "|" + t.priority.name();
        }).collect(Collectors.toList());

        try {
            Files.write(DB, lines, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    private static final class CliTask {
        long id;
        String title;
        String date;
        String description;
        Priority priority;

        CliTask(long id, String title, String date, String description, Priority priority) {
            this.id = id;
            this.title = title == null ? "" : title;
            this.date = date == null ? "" : date;
            this.description = description == null ? "" : description;
            this.priority = priority == null ? Priority.NOT_URGENT_NOT_IMPORTANT : priority;
        }

        enum Priority {
            URGENT_IMPORTANT,
            NOT_URGENT_IMPORTANT,
            URGENT_NOT_IMPORTANT,
            NOT_URGENT_NOT_IMPORTANT
        }

        @Override
        public String toString() {
            return String.format("CliTask{id=%d, title='%s', date='%s', priority=%s}", id, title, date, priority);
        }
    }
}
