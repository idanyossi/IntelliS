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

    @Override public String toDisplayString() { return var + " <- 0"; }

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
        String loop = (label != null && !label.isBlank()) ? label : ctx.freshLabel();
        out.add(new Decrease(loop, var));
        out.add(new JumpNotZero(null, var, loop));
        return out;
    }
}
