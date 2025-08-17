package s.emulator.core;

import java.util.*;
import java.util.function.Function;

public class ExecutionManager {

    private final Program program;
    private final Map<String, Integer> vars = new HashMap<>();
    private int pc = 0;
    private long totalCycles = 0;
    private boolean running = true;

    public ExecutionManager(Program program) {
        this.program = program;
        // Initialize y=0 and all z*=0 implicitly; x* default 0 unless user input sets them.
        setVar("y", 0);
    }

    public Program getProgram() {
        return program;
    }

    public int getPC() {
        return pc;
    }

    public void setPC(int newPc) {
        this.pc = newPc;
    }

    public void incPC() {
        this.pc++;
    }

    public boolean isRunning() {
        return running;
    }

    public void stop() {
        this.running = false;
    }

    public long getTotalCycles() {
        return totalCycles;
    }

    public void addCycles(int c) {
        totalCycles += Math.max(0, c);
    }

    public int getVar(String name) {
        return vars.getOrDefault(name, 0);
    }

    public void setVar(String name, int value) {
        vars.put(name, Math.max(0, value));
    }

    public Map<String, Integer> snapshotVars() {
        Map<String, Integer> out = new LinkedHashMap<>();
        out.put("y", getVar("y"));
        // Collect x*
        vars.keySet().stream()
                .filter(k -> k.startsWith("x"))
                .sorted(Comparator.comparingInt(ExecutionManager::suffixInt))
                .forEach(k -> out.put(k, vars.get(k)));
        // Collect z*
        vars.keySet().stream()
                .filter(k -> k.startsWith("z"))
                .sorted(Comparator.comparingInt(ExecutionManager::suffixInt))
                .forEach(k -> out.put(k, vars.get(k)));
        // Include anything else (rare) in alpha order but after the above
        vars.keySet().stream()
                .filter(k -> !k.equals("y") && !k.startsWith("x") && !k.startsWith("z"))
                .sorted()
                .forEach(k -> out.put(k, vars.get(k)));
        return out;
    }

    public void jumpToLabel(String label) {
        if (label == null || label.isBlank()) throw new IllegalArgumentException("jumpToLabel: label is empty");
        if ("EXIT".equalsIgnoreCase(label)) { stop(); return; }
        var idx = program.lookupLabel(label);
        if (idx.isEmpty()) throw new IllegalStateException("Unknown label: " + label);
        setPC(idx.getAsInt());
    }

    private static int suffixInt(String s) {
        try {
            return Integer.parseInt(s.substring(1));
        } catch (Exception e) {
            return Integer.MAX_VALUE;
        }
    }
    public void addCycles(long c) {
        if (c > 0) totalCycles += c;
    }

    private Function<String, Program> functionResolver;

    public void setFunctionResolver(Function<String, Program> resolver) {
        this.functionResolver = resolver;
    }

    public Program resolveFunction(String name) {
        if (functionResolver == null) {
            throw new IllegalStateException("No function resolver set (needed for QUOTE/JUMP_EQUAL_FUNCTION).");
        }
        Program p = functionResolver.apply(name);
        if (p == null) throw new IllegalArgumentException("Function not found: " + name);
        return p;
    }

    public static class RunOutcome {
        private final int y;
        private final long cycles;
        public RunOutcome(int y, long cycles) { this.y = y; this.cycles = cycles; }
        public int getY() { return y; }
        public long getCycles() { return cycles; }
    }

    public RunOutcome runSubprogram(Program sub, List<Integer> args) {
        ExecutionManager child = new ExecutionManager(sub);
        for (int i = 0; i < args.size(); i++) {
            child.setVar("x" + (i + 1), args.get(i));
        }
        long childCycles = Interpreter.run(child);
        return new RunOutcome(child.getVar("y"), childCycles);
    }
}
