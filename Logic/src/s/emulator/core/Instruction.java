package s.emulator.core;

import s.emulator.core.expansion.ExpansionContext;

import java.util.List;
import java.util.Map;

public interface Instruction {

    String getLabel();

    int getCycles();

    void execute(ExecutionManager em);

    Instruction buildFromXml(String label, String variable, Map<String,String> args);

    default boolean isBasic() { return true; }

    default List<Instruction> expand(ExpansionContext ctx) { return List.of(this); }

    default int degree() {
        if (isBasic()) return 0;
        // expand once with a dummy context; specs guarantee no cycles
        int max = 0;
        for (Instruction c : expand(new ExpansionContext(0, 0, 0))) {
            max = Math.max(max, c.degree());
        }
        return 1 + max;
    }

}
