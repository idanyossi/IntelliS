package ui;

import s.emulator.api.Engine;
import s.emulator.api.dto.Dtos;
import s.emulator.api.engine.EngineImpl;
import s.emulator.core.*;

import java.io.File;
import java.util.*;

public class ConsoleApp {

    private final Engine engine = new EngineImpl();

    public static void main(String[] args) {
        new ConsoleApp().run();
    }

    private void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("""
                === IntelliS Console ===
                1) Load XML file
                2) Show code
                3) Show degree
                4) Run program
                5) Show history
                6) Exit
                """);
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> doLoad(sc);
                    case "2" -> doShowCode();
                    case "3" -> doShowDegree(sc);
                    case "4" -> doRunProgram(sc);
                    case "5" -> doHistory();
                    case "6" -> System.exit(0);
                    default -> System.out.println("Unknown option");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void needsProgram() {
        if (!engine.hasProgram()) throw new IllegalStateException("No valid program loaded.");
    }

    private void doLoad(Scanner sc) throws Exception {
        System.out.print("Enter XML path: ");
        String path = sc.nextLine().trim();
        engine.loadProgram(new File(path));
        System.out.println("Loaded program: " + engine.currentProgramName());
    }

    private void doShowCode() {
        needsProgram();
        Dtos.ProgramSummary s = engine.getProgramSummary();
        ConsolePrinters.printProgramSummary(s);
    }

    private void doShowDegree(Scanner sc) {
        needsProgram();
        int max = engine.getMaxDegree();
        System.out.println("Max degree: " + max);
        System.out.print("Pick degree [0.." + max + "]: ");
        int d = parseIntInRange(sc.nextLine(), 0, max);

        Dtos.ProgramSummary s = engine.getProgramSummary(d);
        Dtos.ExpansionPreview prev = (d > 0) ? engine.previewExpansion(d) : null;
        ConsolePrinters.printProgramSummaryWithParents(s, prev);
    }

    private void doRunProgram(Scanner sc) {
        needsProgram();
        int max = engine.getMaxDegree();
        System.out.println("Max degree: " + max);
        System.out.print("Pick degree [0.." + max + "]: ");
        int d = parseIntInRange(sc.nextLine(), 0, max);

        Dtos.ProgramSummary listing = engine.getProgramSummary(d);
        Dtos.ExpansionPreview prev = (d > 0) ? engine.previewExpansion(d) : null;
        ConsolePrinters.printProgramSummaryWithParents(listing, prev);

        List<String> used = engine.getInputsUsed(d);
        System.out.println("Inputs used in this degree: " + (used.isEmpty() ? "(none)" : String.join(",", used)));
        System.out.print("Enter x's IN ORDER (CSV, e.g., 1,2,3). Blank = none: ");
        String csv = sc.nextLine().trim();

        Map<String,Integer> init = parseSequentialInputs(csv); // x1..xN from CSV length
        Dtos.RunResult rr = engine.run(d, init);

        System.out.println("\nInputs used in program (with values):");
        if (used.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (String x : used) {
                int val = init.getOrDefault(x, 0);
                System.out.println("  " + x + " = " + val);
            }
        }

        System.out.println("\nInputs initialized (even if not used):");
        if (init.isEmpty()) {
            System.out.println("  (none)");
        } else {
            for (var e : init.entrySet()) {
                System.out.println("  " + e.getKey() + " = " + e.getValue());
            }
        }

        // Results
        System.out.println();
        ConsolePrinters.printRunResult(rr);
    }

    private void doHistory() {
        needsProgram();
        var hist = engine.getHistory();
        ConsolePrinters.printHistory(hist);
    }

    private static Map<String,Integer> parseSequentialInputs(String csv) {
        Map<String,Integer> map = new LinkedHashMap<>();
        if (csv == null || csv.isBlank()) return map;
        String[] parts = csv.split(",");
        int idx = 1;
        for (String p : parts) {
            String t = p.trim();
            if (t.isEmpty()) continue;
            int v;
            try { v = Integer.parseInt(t); } catch (NumberFormatException e) { throw new IllegalArgumentException("Bad number: " + t); }
            map.put("x" + idx, Math.max(0, v));
            idx++;
        }
        return map;
    }

    private static int parseIntInRange(String s, int lo, int hi) {
        int v;
        try { v = Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Not a number: " + s); }
        if (v < lo || v > hi) throw new IllegalArgumentException("Out of range [" + lo + ".." + hi + "]: " + v);
        return v;
    }
}

