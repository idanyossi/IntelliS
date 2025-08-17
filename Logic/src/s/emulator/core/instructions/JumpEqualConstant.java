package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class JumpEqualConstant implements Instruction {
    private final String label;
    private final String var;
    private final int k;
    private final String targetLabel;

    public JumpEqualConstant(String label, String var, int k, String targetLabel) {
        this.label = label;
        this.var = var;
        this.k = k;
        this.targetLabel = targetLabel;
    }

    @Override public String getLabel() {return label;}
    @Override public int getCycles() {return 2;}

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());

        int temp = executionManager.getVar(var);
        boolean hitZeroBefore = false;

        for (int i = 0; i < k; i++) {
            if (temp == 0){
                hitZeroBefore = true;
                break;
            }
            temp -= 1;
        }
        if (!hitZeroBefore && temp == 0) {
            executionManager.jumpToLabel(targetLabel);
        } else {
            executionManager.incPC();
        }
    }
}
