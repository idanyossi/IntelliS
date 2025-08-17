package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

import java.util.function.Function;

public final class ZeroVariable implements Instruction {
    private final String label;
    private final String var;

    public ZeroVariable(String label, String var) {
        this.label = label;
        this.var = var;
    }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() {return 1; }

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        int v = executionManager.getVar(var);
        while (v > 0) {
            v -= 1;
        }
        executionManager.setVar(var, v);
        executionManager.incPC();
    }
}
