package ui;

import s.emulator.core.Instruction;
import s.emulator.core.Program;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ExpandPrinter {

    private static final class Node {
        final int originIndex;      // index of the original (degree 0) instruction
        final Instruction ins;
        Node(int originIndex, Instruction ins) { this.originIndex = originIndex; this.ins = ins; }
    }

    public static void printExpandedHorizontally(Program program, int degree) {
        final List<Instruction> original = program.getInstructions();

        // degree 0: print only origin, no tails
        if (degree <= 0) {
            for (int i = 0; i < original.size(); i++) {
                System.out.println(ProgramPrinter.formatOne(i + 1, original.get(i)));
            }
            System.out.println();
            return;
        }

        // Build origin-tagged nodes and expand for D rounds (only synthetics expand)
        List<Node> nodes = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) nodes.add(new Node(i, original.get(i)));

        ExpansionContext ctx = ExpansionContext.fromProgram(program);
        for (int d = 0; d < degree; d++) {
            boolean changed = false;
            List<Node> next = new ArrayList<>();
            for (Node n : nodes) {
                if (n.ins.isBasic()) {
                    next.add(n); // basics remain as-is
                } else {
                    List<Instruction> children = n.ins.expand(ctx);
                    for (Instruction c : children) next.add(new Node(n.originIndex, c));
                    changed = true;
                }
            }
            nodes = next;
            if (!changed) break; // fully basic already
        }

        // Map final positions to each origin (preserve final program order)
        Map<Integer, List<Integer>> byOrigin = new LinkedHashMap<>();
        for (int pos = 0; pos < nodes.size(); pos++) {
            Node n = nodes.get(pos);
            byOrigin.computeIfAbsent(n.originIndex, k -> new ArrayList<>()).add(pos);
        }

        // Print one row per original
        for (int origin = 0; origin < original.size(); origin++) {
            Instruction origIns = original.get(origin);
            StringBuilder line = new StringBuilder(ProgramPrinter.formatOne(origin + 1, origIns));

            List<Integer> positions = byOrigin.get(origin);
            // Decide whether to show a tail:
            // show tail iff the final block is different from "just the original itself"
            boolean showTail = false;
            if (positions != null && !positions.isEmpty()) {
                if (positions.size() > 1) {
                    showTail = true; // clearly expanded to multiple
                } else {
                    // single element: show tail only if it's NOT the same instance as the origin
                    Instruction only = nodes.get(positions.get(0)).ins;
                    if (only != origIns) showTail = true;
                }
            }

            if (showTail) {
                for (int pos : positions) {
                    Instruction child = nodes.get(pos).ins;
                    line.append("  >>>  ").append(ProgramPrinter.formatOne(pos + 1, child));
                }
            }

            System.out.println(line);
        }
        System.out.println();
    }
}
