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

    @Override public String toDisplayString() { return destination + " <- " + source; }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 4; }

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
        String t  = ctx.freshZ();
        String L1 = ctx.freshLabel();
        String L2 = ctx.freshLabel();
        String L3 = ctx.freshLabel();

        out.add(new ZeroVariable(label, destination));

        out.add(new JumpNotZero(null, source, L1));
        out.add(new GotoLabel(null, L3));

        out.add(new Decrease(L1, source));
        out.add(new Increase(null, t));
        out.add(new JumpNotZero(null, source, L1));

        out.add(new Increase(L2, destination));
        out.add(new Increase(null, source));
        out.add(new Decrease(null, t));
        out.add(new JumpNotZero(null, t, L2));

        out.add(new Neutral(L3, destination));
        return out;
    }
}
