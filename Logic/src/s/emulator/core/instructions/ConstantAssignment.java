package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

import java.util.Map;

public final class ConstantAssignment implements Instruction {
    private final String label;
    private final String var;
    private final int k;

    public ConstantAssignment(String label, String var, int k) {
        this.label = label;
        this.var = var;
        this.k = Math.max(0,k);
    }
    private ConstantAssignment() { this.label=null; this.var=null; this.k=0; }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 2; }

    @Override
    public void execute(ExecutionManager executionManager) {
        executionManager.addCycles(getCycles());
        int v = executionManager.getVar(var);
        while (v > 0) {
            v -= 1;
        }
        for (int i = 0; i < k; i++) {
            v += 1;
        }

        executionManager.setVar(var, v);
        executionManager.incPC();
    }
    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("CONSTANT_ASSIGNMENT needs destination <S-Variable>.");
        String val = args.get("constantValue");
        if (val == null || val.isBlank())
            throw new IllegalArgumentException("CONSTANT_ASSIGNMENT requires arg constantValue.");
        int k;
        try { k = Integer.parseInt(val.trim()); if (k < 0) throw new NumberFormatException(); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("constantValue must be a natural number (>=0)."); }
        return new ConstantAssignment(label, variable, k);
    }
}
