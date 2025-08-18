package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

import java.util.Map;

public final class Increase implements Instruction {
    private final String label;     // label attached to THIS instruction (may be null)
    private final String var;

    public Increase(String label, String var) {
        this.label = label;
        this.var = var;
    }
    private Increase() { this.label = null; this.var = null; }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 1; }

    @Override
    public void execute(ExecutionManager em) {
        em.setVar(var, em.getVar(var) + 1);
        em.addCycles(getCycles());
        em.incPC();
    }

    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("INCREASE requires <S-Variable>.");
        return new Increase(label, variable);
    }
}
