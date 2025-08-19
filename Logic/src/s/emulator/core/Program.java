package s.emulator.core;

import s.emulator.core.expansion.ExpansionContext;

import java.util.*;

public final class Program {
    private final String name;
    private final List<Instruction> instructions;
    private final Map<String, Integer> firstLabelIndex;


    public Program(String name, List<Instruction> instructions) {
        this.name = name;
        this.instructions = List.copyOf(instructions);
        this.firstLabelIndex = buildFirstLabelIndex(this.instructions);
    }

    public String getName() { return name; }
    public List<Instruction> getInstructions() { return instructions; }

    public OptionalInt lookupLabel(String label) {
        Integer idx = firstLabelIndex.get(label);
        return idx == null ? OptionalInt.empty() : OptionalInt.of(idx);
    }

    private static Map<String, Integer> buildFirstLabelIndex(List<Instruction> insns) {
        Map<String, Integer> map = new LinkedHashMap<>();
        for (int i = 0; i < insns.size(); i++) {
            String lbl = insns.get(i).getLabel();
            if (lbl != null && !lbl.isBlank() && !map.containsKey(lbl)) {
                map.put(lbl, i); // FIRST wins
            }
        }
        return map;
    }

    public Program expandToDegree(int degree) {
        if (degree <= 0) return this;
        ExpansionContext ctx = ExpansionContext.fromProgram(this);
        List<Instruction> cur = this.instructions;
        for (int d = 0; d < degree; d++) {
            boolean changed = false;
            List<Instruction> next = new ArrayList<>(cur.size());
            for (Instruction ins : cur) {
                if (ins.isBasic()) {
                    next.add(ins);
                } else {
                    List<Instruction> ex = ins.expand(ctx);
                    if (!ex.isEmpty()) {
                        if (ex.get(0).getLabel() == null && ins.getLabel() != null) {
                        }
                    }
                    next.addAll(ex);
                    changed = true;
                }
            }
            cur = next;
            if (!changed) break; // fully basic
        }
        return new Program(this.name, cur);
    }

    public int maxExpansionDegree() {
        int deg = 0;
        Program p = this;
        while (p.instructions.stream().anyMatch(i -> !i.isBasic())) {
            p = p.expandToDegree(1);
            deg++;
            if (deg > 10_000) break; // safety
        }
        return deg;
    }
}
