package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

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
}
