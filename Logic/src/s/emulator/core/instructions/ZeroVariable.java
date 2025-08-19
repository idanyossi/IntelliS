package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ZeroVariable implements Instruction {
    private final String label;
    private final String var;

    public ZeroVariable(String label, String var) {
        this.label = label;
        this.var = var;
    }
    private ZeroVariable() { this.label=null; this.var=null; }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() {return 1; }

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        int v = executionManager.getVar(var);
        while (v > 0) {
            v -= 1;
        }
        executionManager.setVar(var, v);
        executionManager.incPC();
    }
    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("ZERO_VARIABLE requires <S-Variable>.");
        return new ZeroVariable(label, variable);
    }

    @Override
    public boolean isBasic() {
        return false;
    }

    @Override
    public List<Instruction> expand(ExpansionContext ctx) {
        List<Instruction> out = new ArrayList<>();

        final String check = (label != null && !label.isBlank()) ? label : ctx.freshLabel();
        final String dec   = ctx.freshLabel();
        final String done  = ctx.freshLabel();

        out.add(new JumpNotZero(check, var, dec));
        out.add(new GotoLabel(null, done));

        out.add(new Decrease(dec, var));
        out.add(new GotoLabel(null, check));

        out.add(new Neutral(done, var));
        return out;
    }
}
