package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;
import s.emulator.core.expansion.ExpansionContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Assignment implements Instruction {
    private final String label;
    private final String destination;
    private final String source;

    public Assignment(String label, String destination, String source) {
        this.label = label;
        this.destination = destination;
        this.source = source;
    }
    private Assignment() { this.label=null; this.destination=null; this.source=null; }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() {return 4;}

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        int val = executionManager.getVar(source);
        executionManager.setVar(destination, val);
        executionManager.incPC();
    }
    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("ASSIGNMENT needs destination <S-Variable>.");
        String src = args.get("assignedVariable");
        if (src == null || src.isBlank())
            throw new IllegalArgumentException("ASSIGNMENT requires arg assignedVariable.");
        return new Assignment(label, variable, src.trim());
    }

    @Override
    public boolean isBasic() {
        return false;
    }

    @Override
    public List<Instruction> expand(ExpansionContext ctx) {
        List<Instruction> out = new ArrayList<>();

        final String tmp   = ctx.freshZ();
        final String L1    = ctx.freshLabel();
        final String L2    = ctx.freshLabel();
        final String Lafter= ctx.freshLabel();
        final String L3    = ctx.freshLabel();
        final String L4    = ctx.freshLabel();
        final String Lend  = ctx.freshLabel();

        out.add(new ZeroVariable(label, destination));

        out.add(new JumpNotZero(L1, source, L2));
        out.add(new GotoLabel(null, Lafter));
        out.add(new Decrease(L2, source));
        out.add(new Increase(null, tmp));
        out.add(new GotoLabel(null, L1));
        out.add(new Neutral(Lafter, destination));

        out.add(new JumpNotZero(L3, tmp, L4));
        out.add(new GotoLabel(null, Lend));
        out.add(new Increase(L4, source));
        out.add(new Increase(null, destination));
        out.add(new Decrease(null, tmp));
        out.add(new GotoLabel(null, L3));

        out.add(new Neutral(Lend, destination));
        return out;
    }
}
