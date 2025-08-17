package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class JumpZero implements Instruction {
    private final String label;
    private final String var;
    private final String targetLabel;

    public JumpZero(String label, String var, String targetLabel) {
        this.label = label;
        this.var = var;
        this.targetLabel = targetLabel;
    }

    @Override public String getLabel() {return label;}
    @Override public int getCycles() {return 2;}

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        if(executionManager.getVar(var) == 0){
            executionManager.jumpToLabel(getLabel());
        } else {
            executionManager.incPC();
        }
    }
}
