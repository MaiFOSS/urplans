package org.maifoss.urplans;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

@SpringBootApplication
public class UrplansApplication {

    public static void main(String[] args) {
        // start Spring context so TaskService/Repo are available
        ConfigurableApplicationContext ctx = SpringApplication.run(UrplansApplication.class, args);
        try {
            TaskService service = ctx.getBean(TaskService.class);
            // If args present, treat as one-shot CLI command and exit afterwards
            if (args.length > 0) {
                boolean handled = handleFlags(service, args);
                if (!handled) {
                    System.out.println("Unknown args. Supported flags: -i, -s, -d, -p, -l");
                }
            } else {
                // interactive mode
                interactiveLoop(service);
            }
        } finally {
            // graceful shutdown of Spring
            SpringApplication.exit(ctx, () -> 0);
        }
    }

    private static boolean handleFlags(TaskService service, String[] args) {
        List<String> a = Arrays.asList(args);
        if (a.contains("-l")) {
            service.findAll().forEach(System.out::println);
            return true;
        }
        int idx;
        if ((idx = a.indexOf("-i")) >= 0 && idx + 1 < a.size()) {
            handleInsert(service, a.get(idx + 1));
            return true;
        }
        if ((idx = a.indexOf("-s")) >= 0 && idx + 1 < a.size()) {
            handleSearch(service, a.get(idx + 1));
            return true;
        }
        if ((idx = a.indexOf("-d")) >= 0 && idx + 1 < a.size()) {
            handleDeleteById(service, a.get(idx + 1));
            return true;
        }
        if ((idx = a.indexOf("-p")) >= 0 && idx + 1 < a.size()) {
            handleDeletePlan(service, a.get(idx + 1));
            return true;
        }
        // unknown
        return false;
    }

    private static void interactiveLoop(TaskService service) {
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.println("\nEisenhower CLI — choose an option:");
            System.out.println("1) Add task (single-line format)");
            System.out.println("2) List all tasks");
            System.out.println("3) List tasks for a date");
            System.out.println("4) Delete task by id");
            System.out.println("5) Delete plan (all tasks for a date)");
            System.out.println("0) Exit");
            System.out.print("> ");
            String opt = in.nextLine().trim();
            switch (opt) {
                case "1" -> {
                    System.out.print("Enter insert string (TITLE|DESC|START>END|PRIORITY): ");
                    String s = in.nextLine().trim();
                    handleInsert(service, s);
                }
                case "2" -> service.findAll().forEach(System.out::println);
                case "3" -> {
                    System.out.print("Enter date (YYYY-MM-DD): ");
                    String d = in.nextLine().trim();
                    try {
                        LocalDate date = LocalDate.parse(d);
                        service.findByDate(date).forEach(System.out::println);
                    } catch (Exception ex) {
                        System.out.println("Bad date.");
                    }
                }
                case "4" -> {
                    System.out.print("Enter id to delete: ");
                    String idS = in.nextLine().trim();
                    handleDeleteById(service, idS);
                }
                case "5" -> {
                    System.out.print("Enter date to delete (YYYY-MM-DD): ");
                    String dateS = in.nextLine().trim();
                    handleDeletePlan(service, dateS);
                }
                case "0" -> {
                    System.out.println("bye.");
                    return;
                }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private static void handleInsert(TaskService service, String input) {
        String[] parts = input.split("\\|", 4);
        String title = parts.length > 0 ? parts[0].trim() : "";
        String desc = parts.length > 1 ? parts[1].trim() : "";
        String range = parts.length > 2 ? parts[2].trim() : "";
        String prio = parts.length > 3 ? parts[3].trim() : "NOT_URGENT_NOT_IMPORTANT";

        LocalDate start;
        LocalDate end = null;
        try {
            if (range == null || range.isBlank()) {
                start = LocalDate.now();
            } else if (range.contains(">")) {
                String[] r = range.split(">", 2);
                start = LocalDate.parse(r[0]);
                if (r.length > 1 && !r[1].isBlank() && !"FOREVER".equalsIgnoreCase(r[1])) {
                    end = LocalDate.parse(r[1]);
                }
            } else {
                start = LocalDate.parse(range);
            }
        } catch (Exception ex) {
            System.out.println("Bad date/range: " + range);
            return;
        }

        Task.Priority priority;
        try {
            priority = Task.Priority.valueOf(prio);
        } catch (Exception ex) {
            priority = Task.Priority.NOT_URGENT_NOT_IMPORTANT;
        }

        Task t = new Task(null, title, desc, start, end, priority);
        Task saved = service.save(t);
        System.out.println("Saved: " + saved);
    }

    private static void handleSearch(TaskService service, String q) {
        if (q.matches("\\d{4}-\\d{2}-\\d{2}")) {
            LocalDate d = LocalDate.parse(q);
            service.findByDate(d).forEach(System.out::println);
            return;
        }
        if (q.contains("=")) {
            for (String token : q.split("&")) {
                String[] kv = token.split("=", 2);
                if (kv.length != 2) continue;
                if ("title".equalsIgnoreCase(kv[0])) {
                    service.findByTitle(kv[1]).forEach(System.out::println);
                    return;
                } else if ("date".equalsIgnoreCase(kv[0])) {
                    service.findByDate(LocalDate.parse(kv[1])).forEach(System.out::println);
                    return;
                }
            }
        }
        service.findByTitle(q).forEach(System.out::println);
    }

    private static void handleDeleteById(TaskService service, String idS) {
        try {
            Long id = Long.parseLong(idS);
            boolean removed = service.deleteById(id);
            System.out.println(removed ? "Deleted id " + id : "No task with id " + id);
        } catch (NumberFormatException ex) {
            System.out.println("Invalid id.");
        }
    }

    private static void handleDeletePlan(TaskService service, String dateS) {
        try {
            LocalDate d = LocalDate.parse(dateS);
            long removed = service.deleteByDate(d);
            System.out.println("Deleted " + removed + " tasks for " + d);
        } catch (Exception ex) {
            System.out.println("Bad date.");
        }
    }
}
