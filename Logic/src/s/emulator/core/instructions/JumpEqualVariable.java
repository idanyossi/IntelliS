package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

import java.util.Map;

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
    private JumpEqualVariable() { this.label=null; this.Vara=null; this.Varb=null; this.targetLabel=null; }

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
    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("JUMP_EQUAL_VARIABLE requires <S-Variable>.");
        String other = args.get("variableName");
        String tgt   = args.get("JEVariableLabel");
        if (other == null || other.isBlank())
            throw new IllegalArgumentException("JUMP_EQUAL_VARIABLE requires arg variableName.");
        if (tgt == null || tgt.isBlank())
            throw new IllegalArgumentException("JUMP_EQUAL_VARIABLE requires arg JEVariableLabel.");
        return new JumpEqualVariable(label, variable, other.trim(), tgt.trim());
    }
}
