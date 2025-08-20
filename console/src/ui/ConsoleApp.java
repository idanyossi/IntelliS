package ui;

import s.emulator.core.*;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;

import static ui.ExpandPrinter.printExpandedHorizontally;

public class ConsoleApp {

    private Program current;
    private final List<RunRecord> history = new ArrayList<>();


    public final class RunRecord {
        private final int runNo;
        private final int degree;
        private final List<String> xNames;   // x1,x2,...
        private final List<Integer> xValues; // aligned with xNames
        private final int y;
        private final long cycles;

        RunRecord(int runNo, int degree, List<String> xNames, List<Integer> xValues, int y, long cycles) {
            this.runNo = runNo;
            this.degree = degree;
            this.xNames = List.copyOf(xNames);
            this.xValues = List.copyOf(xValues);
            this.y = y;
            this.cycles = cycles;
        }
    }

    public static void main(String[] args) {
        new ConsoleApp().run();
    }

    private void run() {
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("""
                === IntelliS Console ===
                1) Load file
                2) Show code from file
                3) Run program
                4) Run with degree
                5) Show history/statistics
                6) Close program
                """);
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> doLoad(sc);
                    case "2" -> doShow();
                    case "3" -> doRun(sc, 0);          // quiet
                    case "4" -> doRunWithDegree(sc);   // quiet
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

    private void needProgram() {
        if (current == null) throw new IllegalStateException("No valid program loaded.");
    }

    private void doLoad(Scanner sc) throws Exception {
        System.out.print("Enter XML path: ");
        String path = sc.nextLine().trim();
        current = new XmlProgramLoader().load(new File(path));
        history.clear(); // new program -> reset history
        System.out.println("Loaded program: " + current.getName());
    }

    private void doShow() {
        needProgram();
        var code = current.getInstructions();
        ProgramPrinter.printProgram(current, code);
    }

    private void doRunWithDegree(Scanner sc) {
        needProgram();
        int max = current.maxExpansionDegree();
        System.out.println("Max degree: " + max);
        System.out.print("Pick degree [0.." + max + "]: ");
        int d = parseIntInRange(sc.nextLine(), 0, max);

        // Optional preview; comment out if you don't want any code shown before running
        System.out.println("\nExpanded (horizontal) to degree " + d + ":");
        printExpandedHorizontally(current, d);

        doRun(sc, d); // quiet run (results only)
    }

    private void doRun(Scanner sc, int degree) {
        needProgram();

        // Expand first so we can compute exactly which inputs this run needs
        Program toRun = current.expandToDegree(degree);

        // Collect the x-variables used by THIS expanded program (sorted: x1,x2,...)
        List<String> usedXs = collectInputVars(toRun.getInstructions());

        // Prompt user for values for those specific x's (aligned, missing -> 0)
        List<Integer> values = askInputsForXs(sc, usedXs);

        // Execute
        ExecutionManager em = new ExecutionManager(toRun);
        for (int i = 0; i < usedXs.size(); i++) {
            em.setVar(usedXs.get(i), values.get(i));
        }
        Interpreter.run(em);

        // Results only
        System.out.println("y = " + em.getVar("y"));
        System.out.println("Variables:");
        em.snapshotVars().forEach((k, v) -> System.out.println("  " + k + " = " + v));
        System.out.println("Total cycles: " + em.getTotalCycles());

        // Save run in history
        history.add(new RunRecord(
                history.size() + 1,
                degree,
                usedXs,
                values,
                em.getVar("y"),
                em.getTotalCycles()
        ));
    }

    private void doHistory() {
        needProgram();
        if (history.isEmpty()) {
            System.out.println("No runs yet for program: " + current.getName());
            return;
        }
        System.out.println("Run | Degree | Inputs                    | y  | Cycles");
        System.out.println("----+--------+---------------------------+----+--------");
        for (RunRecord r : history) {
            String inputs = r.xNames.isEmpty()
                    ? "-"
                    : formatInputs(r.xNames, r.xValues);
            System.out.printf("%3d | %6d | %-25s | %2d | %6d%n",
                    r.runNo, r.degree, inputs, r.y, r.cycles);
        }
    }

    private static String formatInputs(List<String> names, List<Integer> vals) {
        List<String> pairs = new ArrayList<>(names.size());
        for (int i = 0; i < names.size(); i++) {
            int v = (i < vals.size()) ? vals.get(i) : 0;
            pairs.add(names.get(i) + "=" + v);
        }
        return String.join(", ", pairs);
    }

    /** Collect unique x-variables referenced by the code, sorted by numeric index (x1,x2,x5,...) */
    private static List<String> collectInputVars(List<Instruction> code) {
        TreeSet<String> set = new TreeSet<>(Comparator.comparingInt(ConsoleApp::xIndex));
        for (Instruction ins : code) {
            for (Field f : ins.getClass().getDeclaredFields()) {
                if (f.getType() != String.class) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(ins);
                    if (v instanceof String s && s.startsWith("x")) set.add(s);
                } catch (Exception ignore) {}
            }
        }
        return new ArrayList<>(set);
    }

    /** Prompt user to enter values for the exact x's in order (missing → 0, extra → ignored). */
    private static List<Integer> askInputsForXs(Scanner sc, List<String> usedXs) {
        if (usedXs.isEmpty()) {
            System.out.println("This program does not use any x inputs.");
            return List.of();
        }
        String promptList = String.join(",", usedXs);
        System.out.printf("Please enter values for %s (CSV, in this order): ", promptList);
        String line = sc.nextLine().trim();

        List<Integer> parsed = parseCsvInts(line);
        List<Integer> aligned = new ArrayList<>(usedXs.size());
        for (int i = 0; i < usedXs.size(); i++) {
            aligned.add(i < parsed.size() ? parsed.get(i) : 0);
        }
        return aligned;
    }

    private static List<Integer> parseCsvInts(String csv) {
        if (csv.isBlank()) return List.of();
        String[] parts = csv.split(",");
        List<Integer> out = new ArrayList<>(parts.length);
        for (String p : parts) {
            p = p.trim();
            if (p.isEmpty()) continue;
            try { out.add(Integer.parseInt(p)); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Bad number: " + p); }
        }
        return out;
    }

    private static int parseIntInRange(String s, int lo, int hi) {
        int v;
        try { v = Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Not a number: " + s); }
        if (v < lo || v > hi) throw new IllegalArgumentException("Out of range [" + lo + ".." + hi + "]: " + v);
        return v;
    }

    private static int xIndex(String x) {
        try { return Integer.parseInt(x.substring(1)); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }
}

