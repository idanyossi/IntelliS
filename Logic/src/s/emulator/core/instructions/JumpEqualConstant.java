package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private JumpEqualConstant() { this.label=null; this.var=null; this.k=0; this.targetLabel=null; }

    @Override public String toDisplayString() { return "IF " + var + " = " + k + " GOTO " + targetLabel; }

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
    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("JUMP_EQUAL_CONSTANT requires <S-Variable>.");
        String tgt = args.get("JEConstantLabel");
        String val = args.get("constantValue");
        if (tgt == null || tgt.isBlank())
            throw new IllegalArgumentException("JUMP_EQUAL_CONSTANT requires arg JEConstantLabel.");
        if (val == null || val.isBlank())
            throw new IllegalArgumentException("JUMP_EQUAL_CONSTANT requires arg constantValue.");
        int k;
        try { k = Integer.parseInt(val.trim()); if (k < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("constantValue must be a natural number (>=0)."); }
        return new JumpEqualConstant(label, variable, k, tgt.trim());
    }

    @Override
    public boolean isBasic() {
        return false;
    }

    @Override
    public List<Instruction> expand(ExpansionContext ctx) {
        List<Instruction> out = new ArrayList<>();
        final String t  = ctx.freshZ();       // temp z
        final String L1 = ctx.freshLabel();   // not-equal sink

        out.add(new Assignment(label, t, var));

        for (int i = 0; i < k; i++) {
            out.add(new JumpZero(null, t, L1));
            out.add(new Decrease(null, t));
        }

        out.add(new JumpNotZero(null, t, L1));

        out.add(new GotoLabel(null, targetLabel));

        out.add(new Neutral(L1, "y"));
        return out;
    }
}
