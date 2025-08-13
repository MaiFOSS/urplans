package org.maifoss.urplans.cli;

import org.maifoss.urplans.Task;
import org.maifoss.urplans.Task.Priority;
import org.maifoss.urplans.service.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;

@Component
public class CliRunner implements CommandLineRunner {

    private final TaskService svc;
    private final ConfigurableApplicationContext ctx;

    public CliRunner(TaskService svc, ConfigurableApplicationContext ctx) {
        this.svc = svc;
        this.ctx = ctx;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args == null || args.length == 0) {
            printHelp();
            maybeExit();
            return;
        }

        String cmd = args[0];
        try {
            switch (cmd) {
                case "-i": // insert
                case "--insert":
                    if (args.length < 2) {
                        System.err.println("Usage: -i \"Title|Description|2025-08-13>FOREVER|URGENT_IMPORTANT\"");
                        break;
                    }
                    insertFromString(args[1]);
                    break;

                case "-s": // show by date
                case "--show":
                    if (args.length < 2) {
                        System.err.println("Usage: -s 2025-08-13");
                        break;
                    }
                    showByDate(args[1]);
                    break;

                case "-l": // list all
                case "--list":
                    svc.findAll().forEach(System.out::println);
                    break;

                case "-d": // delete by id
                case "--delete":
                    if (args.length < 2) {
                        System.err.println("Usage: -d <id>");
                        break;
                    }
                    deleteById(args[1]);
                    break;

                case "--search":
                    search(args);
                    break;

                default:
                    System.err.println("Unknown command: " + cmd);
                    printHelp();
            }
        } finally {
            // If app is supposed to be CLI-only, exit the Spring context so the JVM stops.
            maybeExit();
        }
    }

    private void insertFromString(String s) {
        // Title|Description|START>END|PRIORITY
        String[] parts = s.split("\\|", -1);
        String title = parts.length > 0 ? parts[0].trim() : "";
        String desc = parts.length > 1 ? parts[1].trim() : "";
        String range = parts.length > 2 ? parts[2].trim() : "";
        String pr = parts.length > 3 ? parts[3].trim() : null;

        LocalDate date = null;
        if (!range.isBlank()) {
            String[] dr = range.split(">", 2);
            String start = dr.length > 0 ? dr[0].trim() : "";
            if (!start.isBlank() && !"FOREVER".equalsIgnoreCase(start)) {
                try {
                    date = LocalDate.parse(start);
                } catch (DateTimeParseException e) {
                    System.err.println("Invalid start date: " + start);
                    return;
                }
            }
        }

        Task t = new Task();
        t.setTitle(title);
        t.setDescription(desc);
        // IMPORTANT: make sure your entity uses `setDate(...)` — change to setStartDate(...) if needed
        t.setDate(date);

        if (pr != null && !pr.isBlank()) {
            try {
                t.setPriority(Priority.valueOf(pr));
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown priority: " + pr + ". Valid values: " + Arrays.toString(Priority.values()));
                return;
            }
        }

        svc.save(t);
        System.out.println("Inserted: " + t);
    }

    private void showByDate(String dateStr) {
        try {
            LocalDate d = LocalDate.parse(dateStr);
            svc.findByDate(d).forEach(System.out::println);
        } catch (DateTimeParseException e) {
            System.err.println("Invalid date: " + dateStr);
        }
    }

    private void deleteById(String idStr) {
        try {
            Long id = Long.parseLong(idStr);
            boolean ok = svc.deleteById(id);
            System.out.println(ok ? "Deleted " + id : "Not found " + id);
        } catch (NumberFormatException e) {
            System.err.println("Invalid id: " + idStr);
        }
    }

    private void search(String[] args) {
        String title = args.length > 1 ? args[1] : null;
        String date = args.length > 2 ? args[2] : null;
        Priority p = null;
        if (args.length > 3 && args[3] != null && !args[3].isBlank()) {
            try {
                p = Priority.valueOf(args[3]);
            } catch (IllegalArgumentException e) {
                System.err.println("Unknown priority: " + args[3] + ". Valid values: " + Arrays.toString(Priority.values()));
                return;
            }
        }
        int page = 0;
        int size = 20;
        try {
            page = args.length > 4 ? Integer.parseInt(args[4]) : 0;
            size = args.length > 5 ? Integer.parseInt(args[5]) : 20;
        } catch (NumberFormatException e) {
            System.err.println("Invalid page/size - using defaults page=0 size=20");
        }

        svc.search(title, date, p, page, size).forEach(System.out::println);
    }

    private void printHelp() {
        System.out.println("urplans-dbcore CLI:");
        System.out.println(" -i \"Title|Description|2025-08-13>FOREVER|PRIORITY\"  insert");
        System.out.println(" -s 2025-08-13   show tasks for date");
        System.out.println(" -l              list all");
        System.out.println(" -d <id>         delete");
        System.out.println(" --search title date priority page size");
        System.out.println();
        System.out.println("Notes:");
        System.out.println("  * Date format: YYYY-MM-DD");
        System.out.println("  * Priority must be one of: " + Arrays.toString(Priority.values()));
        System.out.println("  * If you want the process to exit after running the command, set");
        System.out.println("    spring.main.web-application-type=none (in application.properties or pass");
        System.out.println("    --spring.main.web-application-type=none on the command line).");
    }

    private void maybeExit() {
        String webType = ctx.getEnvironment().getProperty("spring.main.web-application-type", "").trim();
        if ("none".equalsIgnoreCase(webType)) {
            // exit Spring so the JVM will stop (status 0)
            SpringApplication.exit(ctx, () -> 0);
        }
    }
}
