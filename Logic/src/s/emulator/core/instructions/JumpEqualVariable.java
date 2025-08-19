package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public boolean isBasic() {
        return false;
    }

    @Override
    public List<Instruction> expand(ExpansionContext ctx) {
        List<Instruction> out = new ArrayList<>();

        final String z1   = ctx.freshZ();
        final String z2   = ctx.freshZ();
        final String L1   = ctx.freshLabel();
        final String L2   = ctx.freshLabel();
        final String L3   = ctx.freshLabel();
        final String Lend = ctx.freshLabel();

        out.add(new Assignment(label, z1, Vara));
        out.add(new Assignment(null,  z2, Varb));

        out.add(new JumpNotZero(L1, z1, L2));
        out.add(new JumpNotZero(null, z2, Lend));
        out.add(new GotoLabel(null, targetLabel));

        out.add(new JumpNotZero(L2, z2, L3));
        out.add(new GotoLabel(null, Lend));

        out.add(new Decrease(L3, z1));
        out.add(new Decrease(null, z2));
        out.add(new GotoLabel(null, L1));

        out.add(new Neutral(Lend, Vara));
        return out;
    }
}
