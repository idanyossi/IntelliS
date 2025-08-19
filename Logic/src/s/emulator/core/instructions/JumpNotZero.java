package s.emulator.core.instructions;

import s.emulator.core.ExecutionManager;
import s.emulator.core.Instruction;

import java.util.Map;
import java.util.OptionalInt;

public final class JumpNotZero implements Instruction {
    private final String label;      // label attached to THIS instruction (may be null)
    private final String var;        // V to test
    private final String target;     // label name to jump to (e.g., "L1" or "EXIT")

    public JumpNotZero(String label, String var, String targetLabel) {
        this.label = label;
        this.var = var;
        this.target = targetLabel;
    }
    private JumpNotZero() { this.label=null; this.var=null; this.target=null; }

    @Override public String getLabel() { return label; }
    @Override public int getCycles() { return 2; }

    @Override
    public void execute(ExecutionManager em) {
        int v = em.getVar(var);
        em.addCycles(getCycles());
        if (v != 0) {
            if ("EXIT".equalsIgnoreCase(target)) {
                em.stop();
                return;
            }
            OptionalInt idx = em.getProgram().lookupLabel(target);
            if (idx.isEmpty()) {
                throw new IllegalStateException("Unknown label: " + target);
            }
            em.setPC(idx.getAsInt());
        } else {
            em.incPC();
        }
    }
    @Override
    public Instruction buildFromXml(String label, String variable, Map<String,String> args) {
        if (variable == null || variable.isBlank())
            throw new IllegalArgumentException("JUMP_NOT_ZERO requires <S-Variable>.");
        String tgt = args.get("JNZLabel");
        if (tgt == null || tgt.isBlank())
            throw new IllegalArgumentException("JUMP_NOT_ZERO requires arg JNZLabel.");
        return new JumpNotZero(label, variable, tgt.trim());
    }
}
