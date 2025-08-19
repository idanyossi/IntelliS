package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class JumpZero implements Instruction {
    private final String label;
    private final String var;
    private final String targetLabel;

    public JumpZero(String label, String var, String targetLabel) {
        this.label = label;
        this.var = var;
        this.targetLabel = targetLabel;
    }
    private JumpZero() { this.label=null; this.var=null; this.targetLabel=null; }

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
    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("JUMP_ZERO requires <S-Variable>.");
        String tgt = args.get("JZLabel");
        if (tgt == null || tgt.isBlank())
            throw new IllegalArgumentException("JUMP_ZERO requires arg JZLabel.");
        return new JumpZero(label, variable, tgt.trim());
    }

    @Override
    public boolean isBasic() {
        return false;
    }

    @Override
    public List<Instruction> expand(ExpansionContext ctx) {
        List<Instruction> out = new ArrayList<>();
        final String skip = ctx.freshLabel();

        out.add(new JumpNotZero(label, var, skip));
        out.add(new GotoLabel(null, targetLabel));
        out.add(new Neutral(skip, var));
        return out;
    }
}
