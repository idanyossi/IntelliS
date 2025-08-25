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
                1) Load file
                2) Show code from file
                3) Run program
                4) Run with degree
                5) Show history
                6) Close program
                """);
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> doLoad(sc);
                    case "2" -> doShow();
                    case "3" -> doRun(sc, 0);
                    case "4" -> doRunWithDegree(sc);
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

    private void doShow() {
        needsProgram();
        Dtos.ProgramSummary sum = engine.getProgramSummary();
        ConsolePrinters.printProgramSummary(sum);
    }

    private void doRunWithDegree(Scanner sc) {
        needsProgram();
        int max = engine.getMaxDegree();
        System.out.println("Max degree: " + max);
        System.out.print("Pick degree [0.." + max + "]: ");
        int d = parseIntInRange(sc.nextLine(), 0, max);

        Dtos.ExpansionPreview prev = engine.previewExpansion(d);
        ConsolePrinters.printExpansion(prev);

        Map<String,Integer> inputs = askInputsFor(sc, engine.getInputsUsed(d));

        Dtos.RunResult rr = engine.run(d, inputs);
        ConsolePrinters.printRunResult(rr);
    }

    private void doRun(Scanner sc, int degree) {
        needsProgram();

        Map<String,Integer> inputs = askInputsFor(sc, engine.getInputsUsed(0));
        Dtos.RunResult rr = engine.run(degree, inputs);

        ConsolePrinters.printRunResult(rr);
    }

    private void doHistory() {
        needsProgram();
        List<Dtos.RunHistoryEntry> hist = engine.getHistory();
        ConsolePrinters.printHistory(hist);
    }

    private static Map<String,Integer> askInputsFor(Scanner sc, List<String> xNamesInOrder) {
        Map<String,Integer> map = new LinkedHashMap<>();

        if (xNamesInOrder == null || xNamesInOrder.isEmpty()) {
            System.out.println("No inputs required for this degree.");
            return map;
        }

        System.out.println("Please enter values for: " + String.join(",", xNamesInOrder));
        System.out.print("CSV (e.g., 4,7,0). Blank = all zeros: ");
        String line = sc.nextLine().trim();

        if (line.isEmpty()) {
            for (String x : xNamesInOrder) map.put(x, 0);
            return map;
        }

        String[] parts = line.split(",");
        for (int i = 0; i < xNamesInOrder.size(); i++) {
            String token = (i < parts.length) ? parts[i].trim() : "0";
            int val;
            try { val = Integer.parseInt(token); }
            catch (NumberFormatException e) { throw new IllegalArgumentException("Bad number: " + token); }
            map.put(xNamesInOrder.get(i), Math.max(0, val));
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

