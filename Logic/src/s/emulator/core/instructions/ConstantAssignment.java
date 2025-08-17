package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class ConstantAssignment implements Instruction {
    private final String label;
    private final String var;
    private final int k;

    public ConstantAssignment(String label, String var, int k) {
        this.label = label;
        this.var = var;
        this.k = Math.max(0,k);
    }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 2; }

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        int v = executionManager.getVar(var);
        while (v > 0) {
            v -= 1;
        }
        for (int i = 0; i < k; i++) {
            v += 1;
        }

        executionManager.setVar(var, v);
        executionManager.incPC();
    }
}
