package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

import java.util.Map;

public final class NoOp implements Instruction {
    private final String label;
    private final String var; // per spec: "V <- V" (we’ll just read it and write back)

    public NoOp(String label, String var) {
        this.label = label;
        this.var = var;
    }
    private NoOp() { this.label = null; this.var = null; }


    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 1; }

    @Override
    public void execute(ExecutionManager em) {
        em.setVar(var, em.getVar(var)); // explicit self-assign
        em.addCycles(getCycles());
        em.incPC();
    }

    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("NEUTRAL requires <S-Variable>.");
        return new NoOp(label, variable);
    }
}
