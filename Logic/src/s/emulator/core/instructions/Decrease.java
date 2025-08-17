package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class Decrease implements Instruction {
    private final String label;
    private final String var;

    public Decrease(String label, String var) {
        this.label = label;
        this.var = var;
    }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 1; }

    @Override
    public void execute(ExecutionManager em) {
        int v = em.getVar(var);
        em.setVar(var, Math.max(0, v - 1));
        em.addCycles(getCycles());
        em.incPC();
    }
}
