package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class GotoLabel implements Instruction {
    private final String label;
    private final String targetLabel;

    public GotoLabel(String label, String targetLabel) {
        this.label = label;
        this.targetLabel = targetLabel;
    }
    private GotoLabel() { this.label=null; this.targetLabel=null; }

    @Override public String getLabel() {return label;}
    @Override public int getCycles() {return 1;}

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        executionManager.jumpToLabel(targetLabel);
    }

    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        String tgt = args.get("gotoLabel");
        if (tgt == null || tgt.isBlank())
            throw new IllegalArgumentException("GOTO_LABEL requires arg gotoLabel.");
        return new GotoLabel(label, tgt.trim());
    }

    @Override
    public boolean isBasic() {
        return false;
    }

    @Override
    public List<Instruction> expand(ExpansionContext ctx) {
        List<Instruction> out = new ArrayList<>();
        final String t = ctx.freshZ();
        out.add(new Increase(label, t));
        out.add(new JumpNotZero(null, t, targetLabel));
        return out;
    }
}
