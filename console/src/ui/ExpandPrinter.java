package ui;

import s.emulator.core.expansion.ExpansionContext;
import s.emulator.core.Instruction;
import s.emulator.core.Program;

import java.util.*;

public final class ExpandPrinter {

    private static final class Node {
        final int originIndex;
        final Instruction ins;
        Node(int originIndex, Instruction ins) { this.originIndex = originIndex; this.ins = ins; }
    }

    public static void printExpandedHorizontally(Program program, int degree) {
        final List<Instruction> original = program.getInstructions();

        // Degree 0: show only originals (no >>> tail)
        if (degree <= 0) {
            for (int i = 0; i < original.size(); i++) {
                System.out.println(ProgramPrinter.formatOne(i + 1, original.get(i))); // uses ins.isBasic()
            }
            System.out.println();
            return;
        }

        // Expand D rounds
        List<Node> nodes = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) nodes.add(new Node(i, original.get(i)));

        ExpansionContext ctx = ExpansionContext.fromProgram(program);
        for (int d = 0; d < degree; d++) {
            boolean changed = false;
            List<Node> next = new ArrayList<>();
            for (Node n : nodes) {
                if (n.ins.isBasic()) {
                    next.add(n); // basics stay as-is
                } else {
                    List<Instruction> kids = n.ins.expand(ctx);
                    for (Instruction k : kids) next.add(new Node(n.originIndex, k));
                    changed = true;
                }
            }
            nodes = next;
            if (!changed) break; // already all-basic
        }

        // Group final positions by original instruction
        Map<Integer, List<Integer>> byOrigin = new LinkedHashMap<>();
        for (int pos = 0; pos < nodes.size(); pos++) {
            byOrigin.computeIfAbsent(nodes.get(pos).originIndex, k -> new ArrayList<>()).add(pos);
        }

        // Print horizontally: origin  >>> child1 >>> child2 ...
        for (int origin = 0; origin < original.size(); origin++) {
            Instruction origIns = original.get(origin);

            // Left segment: exactly as-is (ProgramPrinter uses ins.isBasic())
            StringBuilder line = new StringBuilder(ProgramPrinter.formatOne(origin + 1, origIns));

            List<Integer> positions = byOrigin.get(origin);
            boolean showTail = false;
            if (positions != null && !positions.isEmpty()) {
                if (positions.size() > 1) {
                    showTail = true;
                } else {
                    // one element: show only if the element is not literally the same instance (i.e., actually expanded)
                    Instruction only = nodes.get(positions.get(0)).ins;
                    if (only != origIns) showTail = true;
                }
            }

            if (showTail) {
                for (int pos : positions) {
                    Instruction child = nodes.get(pos).ins;
                    // Tail uses ProgramPrinter.formatOne(child) → calls child.isBasic() to decide (B|S)
                    line.append("  >>>  ").append(ProgramPrinter.formatOne(pos + 1, child));
                }
            }

            System.out.println(line);
        }
        System.out.println();
    }
}