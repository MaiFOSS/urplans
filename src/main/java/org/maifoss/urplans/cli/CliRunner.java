package org.maifoss.urplans.cli;

import org.maifoss.urplans.Task;
import org.maifoss.urplans.Task.Priority;
import org.maifoss.urplans.service.TaskService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Component
public class CliRunner implements CommandLineRunner {

    private final TaskService svc;

    public CliRunner(TaskService svc) {
        this.svc = svc;
    }

    @Override
    public void run(String... args) throws Exception {
        if (args == null || args.length == 0) {
            // nothing to do; just exit (or show help)
            printHelp();
            return;
        }

        String cmd = args[0];
        switch (cmd) {
            case "-i": // insert
            case "--insert":
                // expect one string argument: Title|Description|START>END|PRIORITY
                if (args.length < 2) {
                    System.err.println("Usage: -i \"Title|Description|2025-08-13>FOREVER|URGENT_IMPORTANT\"");
                    return;
                }
                insertFromString(args[1]);
                break;

            case "-s": // show by date
            case "--show":
                if (args.length < 2) {
                    System.err.println("Usage: -s 2025-08-13");
                    return;
                }
                LocalDate d = LocalDate.parse(args[1]);
                svc.findByDate(d).forEach(System.out::println);
                break;

            case "-l": // list all
            case "--list":
                svc.findAll().forEach(System.out::println);
                break;

            case "-d": // delete by id
            case "--delete":
                if (args.length < 2) {
                    System.err.println("Usage: -d <id>");
                    return;
                }
                Long id = Long.parseLong(args[1]);
                boolean ok = svc.deleteById(id);
                System.out.println(ok ? "Deleted " + id : "Not found " + id);
                break;

            case "--search":
                // example: --search title date priority page size
                String title = args.length > 1 ? args[1] : null;
                String date = args.length > 2 ? args[2] : null;
                Priority p = null;
                if (args.length > 3 && args[3] != null && !args[3].isBlank()) {
                    p = Priority.valueOf(args[3]);
                }
                int page = args.length > 4 ? Integer.parseInt(args[4]) : 0;
                int size = args.length > 5 ? Integer.parseInt(args[5]) : 20;
                svc.search(title, date, p, page, size).forEach(System.out::println);
                break;

            default:
                System.err.println("Unknown command: " + cmd);
                printHelp();
        }
    }

    private void insertFromString(String s) {
        // parse Title|Description|START>END|PRIORITY — keep it simple:
        String[] parts = s.split("\\|", -1);
        String title = parts.length > 0 ? parts[0] : "";
        String desc = parts.length > 1 ? parts[1] : "";
        String range = parts.length > 2 ? parts[2] : "";
        String pr = parts.length > 3 ? parts[3] : null;

        LocalDate date = null;
        if (!range.isBlank()) {
            // you used single date like 2025-08-13>FOREVER before; we'll pick start
            String[] dr = range.split(">", 2);
            if (dr.length > 0 && !dr[0].isBlank() && !dr[0].equals("FOREVER")) {
                date = LocalDate.parse(dr[0]);
            }
        }

        Task t = new Task(); // assumes setters present
        t.setTitle(title);
        t.setDescription(desc);
        t.setDate(date); // match your Task field name (date or startDate)
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
    }
}
