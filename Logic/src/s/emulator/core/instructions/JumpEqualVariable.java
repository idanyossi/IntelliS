package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

public final class JumpEqualVariable implements Instruction {
    private final String label;
    private final String Vara;
    private final String Varb;
    private final String targetLabel;

    public JumpEqualVariable(String label, String a, String b, String targetLabel) {
        this.label = label;
        this.Vara = a;
        this.Varb = b;
        this.targetLabel = targetLabel;
    }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() {  return 2; }

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        int a = executionManager.getVar(Vara);
        int b = executionManager.getVar(Varb);

        while (a > 0 && b > 0) {
            a--;
            b--;
        }
        if (a == 0 && b == 0) {
            executionManager.jumpToLabel(targetLabel);
        } else {
            executionManager.incPC();
        }
    }
}
