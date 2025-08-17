package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class Increase implements Instruction {
    private final String label;     // label attached to THIS instruction (may be null)
    private final String var;

    public Increase(String label, String var) {
        this.label = label;
        this.var = var;
    }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 1; }

    @Override
    public void execute(ExecutionManager em) {
        em.setVar(var, em.getVar(var) + 1);
        em.addCycles(getCycles());
        em.incPC();
    }
}
