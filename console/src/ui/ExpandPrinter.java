package ui;

import s.emulator.core.Instruction;
import s.emulator.core.Program;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ExpandPrinter {
    private static final class Node {
        final int originIndex;      // index of the original (degree 0) instruction
        final Instruction ins;
        Node(int originIndex, Instruction ins) { this.originIndex = originIndex; this.ins = ins; }
    }

    public static void printExpandedHorizontally(Program program, int degree) {
        // 1) Take a snapshot of original code (degree 0).
        final List<Instruction> original = program.getInstructions();

        // 2) Attach an "origin index" to every instruction, then expand for D rounds.
        List<Node> nodes = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) nodes.add(new Node(i, original.get(i)));

        if (degree > 0) {
            ExpansionContext ctx = ExpansionContext.fromProgram(program);
            for (int d = 0; d < degree; d++) {
                boolean changed = false;
                List<Node> next = new ArrayList<>();
                for (Node n : nodes) {
                    if (n.ins.isBasic()) {
                        next.add(n);
                    } else {
                        List<Instruction> children = n.ins.expand(ctx);
                        for (Instruction c : children) next.add(new Node(n.originIndex, c));
                        changed = true;
                    }
                }
                nodes = next;
                if (!changed) break; // fully basic
            }
        }

        // 3) Number final list 1..N (the #<number> shown for children).
        //    Group children by their origin index, preserving order of appearance.
        Map<Integer, List<Integer>> byOrigin = new LinkedHashMap<>();
        for (int pos = 0; pos < nodes.size(); pos++) {
            Node n = nodes.get(pos);
            byOrigin.computeIfAbsent(n.originIndex, k -> new ArrayList<>()).add(pos);
        }

        // 4) Print one row per original instruction.
        for (int origin = 0; origin < original.size(); origin++) {
            StringBuilder line = new StringBuilder();
            // original segment uses its original number (origin+1)
            line.append(ProgramPrinter.formatOne(origin + 1, original.get(origin)));

            List<Integer> positions = byOrigin.get(origin);
            if (positions != null && !positions.isEmpty()) {
                for (int pos : positions) {
                    Instruction child = nodes.get(pos).ins;
                    line.append("  >>>  ").append(ProgramPrinter.formatOne(pos + 1, child));
                }
            } else {
                // nothing expanded out of this instruction at this degree
                // (still print only the origin segment)
            }

            System.out.println(line);
        }
        System.out.println();
    }
}
