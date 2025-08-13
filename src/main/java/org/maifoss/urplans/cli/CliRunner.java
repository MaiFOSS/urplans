package org.maifoss.urplans.cli;

import org.maifoss.urplans.Task;
import org.maifoss.urplans.Task.Priority;
import org.maifoss.urplans.service.TaskService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@Order(Ordered.LOWEST_PRECEDENCE) // ensure CLI runs after seeding/run-once runners
public class CliRunner implements ApplicationRunner {

    private final TaskService svc;

    public CliRunner(TaskService svc) {
        this.svc = svc;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // Non-option args are your app-level arguments (not Spring properties)
        List<String> nonOptionArgs = args.getNonOptionArgs();
        if (nonOptionArgs == null || nonOptionArgs.isEmpty()) {
            printHelp();
            return;
        }

        String cmd = nonOptionArgs.get(0);
        switch (cmd) {
            case "-i":
            case "--insert":
                if (nonOptionArgs.size() < 2) {
                    System.err.println("Usage: -i \"Title|Description|2025-08-13>FOREVER|URGENT_IMPORTANT\"");
                    return;
                }
                insertFromString(nonOptionArgs.get(1));
                break;

            case "-s":
            case "--show":
                if (nonOptionArgs.size() < 2) {
                    System.err.println("Usage: -s 2025-08-13");
                    return;
                }
                LocalDate d = LocalDate.parse(nonOptionArgs.get(1));
                svc.findByDate(d).forEach(System.out::println);
                break;

            case "-l":
            case "--list":
                svc.findAll().forEach(System.out::println);
                break;

            case "-d":
            case "--delete":
                if (nonOptionArgs.size() < 2) {
                    System.err.println("Usage: -d <id>");
                    return;
                }
                Long id = Long.parseLong(nonOptionArgs.get(1));
                boolean ok = svc.deleteById(id);
                System.out.println(ok ? "Deleted " + id : "Not found " + id);
                break;

            case "--search":
                String title = nonOptionArgs.size() > 1 ? nonOptionArgs.get(1) : null;
                String date = nonOptionArgs.size() > 2 ? nonOptionArgs.get(2) : null;
                Priority p = null;
                if (nonOptionArgs.size() > 3 && nonOptionArgs.get(3) != null && !nonOptionArgs.get(3).isBlank()) {
                    try { p = Priority.valueOf(nonOptionArgs.get(3)); } catch (Exception ignored) {}
                }
                int page = nonOptionArgs.size() > 4 ? Integer.parseInt(nonOptionArgs.get(4)) : 0;
                int size = nonOptionArgs.size() > 5 ? Integer.parseInt(nonOptionArgs.get(5)) : 20;
                svc.search(title, date, p, page, size).forEach(System.out::println);
                break;

            default:
                System.err.println("Unknown command: " + cmd);
                printHelp();
        }
    }

    private void insertFromString(String s) {
        String[] parts = s.split("\\|", -1);
        String title = parts.length > 0 ? parts[0] : "";
        String desc = parts.length > 1 ? parts[1] : "";
        String range = parts.length > 2 ? parts[2] : "";
        String pr = parts.length > 3 ? parts[3] : null;

        LocalDate date = null;
        if (!range.isBlank()) {
            String[] dr = range.split(">", 2);
            if (dr.length > 0 && !dr[0].isBlank() && !"FOREVER".equals(dr[0])) {
                date = LocalDate.parse(dr[0]);
            }
        }

        Task t = new Task();
        t.setTitle(title);
        t.setDescription(desc);
        t.setDate(date);
        if (pr != null && !pr.isBlank()) {
            try { t.setPriority(Priority.valueOf(pr)); } catch (Exception ignored) {}
        }
        svc.save(t);
        System.out.println("Inserted: " + t);
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
        System.out.println(" * Date format: YYYY-MM-DD");
        System.out.println(" * Priority must be one of: [URGENT_IMPORTANT, NOT_URGENT_IMPORTANT, URGENT_NOT_IMPORTANT, NOT_URGENT_NOT_IMPORTANT]");
        System.out.println(" * To run as non-web CLI generally use: -Dspring.main.web-application-type=none");
    }
}
