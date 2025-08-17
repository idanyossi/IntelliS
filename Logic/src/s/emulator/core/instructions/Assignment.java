package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class Assignment implements Instruction {
    private final String label;
    private final String destination;
    private final String source;

    public Assignment(String label, String destination, String source) {
        this.label = label;
        this.destination = destination;
        this.source = source;
    }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() {return 4;}

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        int val = executionManager.getVar(source);
        executionManager.setVar(destination, val);
        executionManager.incPC();
    }
}
