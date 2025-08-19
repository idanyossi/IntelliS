package ui;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Interpreter;
import s.emulator.core.Program;
import s.emulator.core.XmlProgramLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static ui.ExpandPrinter.printExpandedHorizontally;

public class ConsoleApp {

    private Program current;

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
                5) Close program
                """);
            System.out.print("> ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> doLoad(sc);
                    case "2" -> doShow();
                    case "3" -> doRun(sc, 0);
                    case "4" -> doRunWithDegree(sc);
                    case "5" -> System.exit(0);
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

        System.out.println("\nExpanded (horizontal) to degree " + d + ":");
        printExpandedHorizontally(current, d);
        doRun(sc, d);
    }

    private void doRun(Scanner sc, int degree) {
        needProgram();

        // 1) Ask inputs (CSV)
        var inputs = askInputs(sc);

        // 2) Expand to degree
        Program toRun = current.expandToDegree(degree);

        // 3) Run
        ExecutionManager em = new ExecutionManager(toRun);
        for (int i = 0; i < inputs.size(); i++) {
            em.setVar("x" + (i + 1), inputs.get(i));
        }
        Interpreter.run(em);

        System.out.println("\nProgram actually executed (degree " + degree + "):");
        ProgramPrinter.printProgram(toRun, toRun.getInstructions());

        // 5) Show results
        System.out.println("y = " + em.getVar("y"));
        System.out.println("Variables:");
        em.snapshotVars().forEach((k, v) -> System.out.println("  " + k + " = " + v));
        System.out.println("Total cycles: " + em.getTotalCycles());
    }

    private static List<Integer> askInputs(Scanner sc) {
        System.out.print("Inputs CSV (e.g., 4,7,0) — blank for none: ");
        String line = sc.nextLine().trim();
        if (line.isEmpty()) return List.of();
        String[] parts = line.split(",");
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
}

