package ui;

import s.emulator.api.dto.Dtos;

import java.util.List;

public class ConsolePrinters {
    private ConsolePrinters() {}

    // ========== Program ==========
    static void printProgramSummary(Dtos.ProgramSummary s) {
        System.out.println("Program: " + s.getProgramName());
        System.out.println("Inputs: " + String.join(", ", s.getInputsUsed()));
        System.out.println("Labels: " + String.join(", ", s.getLabelsUsed()));
        System.out.println();

        for (Dtos.InstructionLine line : s.getInstructions()) {
            System.out.println(formatLine(line));
        }
        System.out.println();
    }

    // ========== Expansion (horizontal) ==========
    static void printExpansion(Dtos.ExpansionPreview p) {
        System.out.println("\nExpanded (horizontal) to degree " + p.getDegree() + ":");
        for (Dtos.ExpansionRow row : p.getRows()) {
            StringBuilder sb = new StringBuilder();
            sb.append(formatLine(row.getOrigin()));
            for (Dtos.InstructionLine child : row.getTail()) {
                sb.append("  >>>  ").append(formatLine(child));
            }
            System.out.println(sb);
        }
        System.out.println();
    }

    // ========== Run result ==========
    static void printRunResult(Dtos.RunResult rr) {
        System.out.println("Program: " + rr.getProgramName());
        System.out.println("Degree: " + rr.getDegree());
        System.out.println("y = " + rr.getY());
        System.out.println("Variables:");
        for (Dtos.NameValue nv : rr.getVariables()) {
            System.out.println("  " + nv.getName() + " = " + nv.getValue());
        }
        System.out.println("Total cycles: " + rr.getCycles());
    }

    // ========== History ==========
    static void printHistory(List<Dtos.RunHistoryEntry> hist) {
        if (hist.isEmpty()) {
            System.out.println("No runs recorded yet.");
            return;
        }
        System.out.println("Run history:");
        for (Dtos.RunHistoryEntry e : hist) {
            String in = joinInputs(e.getInputs());
            System.out.printf("#%d  degree=%d  inputs=[%s]  y=%d  cycles=%d%n",
                    e.getRunNo(), e.getDegree(), in, e.getY(), e.getCycles());
        }
    }

    // ========== helpers ==========
    private static String formatLine(Dtos.InstructionLine l) {
        String kind = l.isBasic() ? "B" : "S";
        String label = pad5(l.getLabel());
        String display = l.getDisplay();
        int cycles = l.getCycles();
        // "#<n> (B|S) [LABEL] <display> (cycles)"
        return String.format("#%d (%s) [%s] %s (%d)", l.getLineNumber(), kind, label, display, cycles);
    }

    private static String pad5(String L) {
        if (L == null || L.isBlank()) return "     ";
        return String.format("%-5s", L);
    }

    private static String joinInputs(List<Dtos.NameValue> in) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < in.size(); i++) {
            var nv = in.get(i);
            if (i > 0) sb.append(',');
            sb.append(nv.getName()).append('=').append(nv.getValue());
        }
        return sb.toString();
    }
}
