package s.emulator.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;

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
}
