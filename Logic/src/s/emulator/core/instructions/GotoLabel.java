package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class GotoLabel implements Instruction {
    private final String label;
    private final String targetLabel;

    public GotoLabel(String label, String targetLabel) {
        this.label = label;
        this.targetLabel = targetLabel;
    }

    @Override public String getLabel() {return label;}
    @Override public int getCycles() {return 1;}

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        executionManager.jumpToLabel(targetLabel);
    }
}
