package s.emulator.api.engine;

import s.emulator.api.Engine;
import s.emulator.api.dto.Dtos;
import s.emulator.core.*;
import s.emulator.core.expansion.ExpansionContext;
import s.emulator.core.instructions.Decrease;
import s.emulator.core.instructions.Increase;
import s.emulator.core.instructions.JumpNotZero;
import s.emulator.core.instructions.Neutral;

import java.io.File;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

public class EngineImpl implements Engine {
    private Program current;
    private final List<Dtos.RunHistoryEntry> history = new ArrayList<>();


    @Override
    public void loadProgram(File xml) throws Exception {
        current = new XmlProgramLoader().load(xml);
        history.clear();
    }

    @Override
    public String currentProgramName() {
        return current == null ? null : current.getName();
    }

    @Override
    public boolean hasProgram() {
        return current != null;
    }

    @Override
    public Dtos.ProgramSummary getProgramSummary() {
        ensureLoaded();
        var code = current.getInstructions();
        return Dtos.ProgramSummary.of(
                current.getName(),
                collectInputs(code),
                collectLabels(code),
                toLines(code)
        );
    }

    @Override
    public int getMaxDegree() {
        ensureLoaded();
        return current.maxExpansionDegree();
    }

    @Override
    public List<String> getInputsUsed(int degree) {
        ensureLoaded();
        Program p = current.expandToDegree(Math.max(0, degree));
        return collectInputs(p.getInstructions());
    }

    @Override
    public Dtos.ExpansionPreview previewExpansion(int degree) {
        ensureLoaded();
        final int D = Math.max(0, degree);
        final List<Instruction> original = current.getInstructions();

        if (D == 0) {
            List<Dtos.ExpansionRow> rows = new ArrayList<>(original.size());
            for (int i = 0; i < original.size(); i++) {
                rows.add(Dtos.ExpansionRow.of(toLine(i + 1, original.get(i)), List.of()));
            }
            return Dtos.ExpansionPreview.of(0, rows);
        }

        final class Node {
            final int originIdx;
            final Instruction ins;
            Node(int originIdx, Instruction ins) { this.originIdx = originIdx; this.ins = ins; }
        }

        List<Node> nodes = new ArrayList<>(original.size());
        for (int i = 0; i < original.size(); i++) nodes.add(new Node(i, original.get(i)));

        ExpansionContext ctx = ExpansionContext.fromProgram(current);
        for (int d = 0; d < D; d++) {
            boolean changed = false;
            List<Node> next = new ArrayList<>();
            for (Node n : nodes) {
                if (n.ins.isBasic()) {
                    next.add(n);
                } else {
                    for (Instruction k : n.ins.expand(ctx)) {
                        next.add(new Node(n.originIdx, k));
                    }
                    changed = true;
                }
            }
            nodes = next;
            if (!changed) break; // already all-basic
        }

        Map<Integer, List<Integer>> byOrigin = new LinkedHashMap<>();
        for (int pos = 0; pos < nodes.size(); pos++) {
            byOrigin.computeIfAbsent(nodes.get(pos).originIdx, k -> new ArrayList<>()).add(pos);
        }

        List<Dtos.ExpansionRow> rows = new ArrayList<>(original.size());
        for (int origin = 0; origin < original.size(); origin++) {
            Instruction origIns = original.get(origin);
            var originLine = toLine(origin + 1, origIns);

            List<Dtos.InstructionLine> tail = new ArrayList<>();
            List<Integer> positions = byOrigin.get(origin);
            if (positions != null && !positions.isEmpty()) {
                boolean showTail;
                if (positions.size() > 1) {
                    showTail = true;
                } else {
                    Instruction only = nodes.get(positions.get(0)).ins;
                    showTail = (only != origIns);
                }
                if (showTail) {
                    for (int pos : positions) {
                        tail.add(toLine(pos + 1, nodes.get(pos).ins));
                    }
                }
            }
            rows.add(Dtos.ExpansionRow.of(originLine, tail));
        }

        return Dtos.ExpansionPreview.of(D, rows);
    }


    @Override
    public Dtos.RunResult run(int degree, Map<String, Integer> inputsByName) {
        ensureLoaded();
        final int D = Math.max(0, degree);
        Program toRun = current.expandToDegree(D);

        ExecutionManager em = new ExecutionManager(toRun);
        if (inputsByName != null) {
            for (var e : inputsByName.entrySet()) {
                em.setVar(e.getKey(), e.getValue() == null ? 0 : Math.max(0, e.getValue()));
            }
        }
        Interpreter.run(em);

        List<Dtos.NameValue> vars = em.snapshotVars().entrySet().stream()
                .map(e -> Dtos.NameValue.of(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        Dtos.RunResult result = Dtos.RunResult.of(
                current.getName(),
                D,
                em.getVar("y"),
                em.getTotalCycles(),
                vars
        );

        List<String> usedXs = getInputsUsed(D);
        List<Dtos.NameValue> inputsForHistory = new ArrayList<>(usedXs.size());
        for (String x : usedXs) {
            int v = (inputsByName == null) ? 0 : inputsByName.getOrDefault(x, 0);
            inputsForHistory.add(Dtos.NameValue.of(x, v));
        }
        history.add(Dtos.RunHistoryEntry.of(
                history.size() + 1,
                D,
                inputsForHistory,
                result.getY(),
                result.getCycles()
        ));

        return result;
    }

    @Override
    public List<Dtos.RunHistoryEntry> getHistory() {
        return List.copyOf(history);
    }

    @Override
    public void clearHistory() {
        history.clear();
    }

    private void ensureLoaded() {
        if (current == null) throw new IllegalStateException("No valid program loaded.");
    }

    private List<Dtos.InstructionLine> toLines(List<Instruction> code) {
        List<Dtos.InstructionLine> out = new ArrayList<>(code.size());
        for (int i = 0; i < code.size(); i++) out.add(toLine(i + 1, code.get(i)));
        return out;
    }

    private Dtos.InstructionLine toLine(int number, Instruction ins) {
        return Dtos.InstructionLine.of(
                number,
                ins.isBasic(),
                ins.getLabel(),
                displayOf(ins),
                ins.getCycles()
        );
    }

    private String displayOf(Instruction ins) {
        String s = safe(ins.toDisplayString());
        if (!s.isBlank() && !s.equals(ins.getClass().getSimpleName())) return s;

        // Fallbacks for the four basics
        if (ins instanceof Increase)    return fmtVar(ins, "var") + " <- " + fmtVar(ins, "var") + " + 1";
        if (ins instanceof Decrease)    return fmtVar(ins, "var") + " <- " + fmtVar(ins, "var") + " - 1";
        if (ins instanceof Neutral)        return fmtVar(ins, "var") + " <- " + fmtVar(ins, "var");
        if (ins instanceof JumpNotZero) return "IF " + fmtVar(ins, "var") + " != 0 GOTO " + getStr(ins, "target");

        // Last resort
        return ins.getClass().getSimpleName();
    }

    private static String safe(String s) { return s == null ? "" : s; }

    private static String fmtVar(Instruction ins, String field) {
        String v = getStr(ins, field);
        return (v == null || v.isBlank()) ? "?" : v;
    }

    private static String getStr(Object o, String field) {
        try {
            Field f = o.getClass().getDeclaredField(field);
            f.setAccessible(true);
            Object v = f.get(o);
            return v instanceof String ? (String) v : null;
        } catch (Exception e) {
            return null;
        }
    }

    private List<String> collectInputs(List<Instruction> code) {
        TreeSet<String> set = new TreeSet<>(Comparator.comparingInt(EngineImpl::xIndex));
        for (Instruction ins : code) {
            for (Field f : ins.getClass().getDeclaredFields()) {
                if (f.getType() != String.class) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(ins);
                    if (v instanceof String s && s.startsWith("x")) set.add(s);
                } catch (Exception ignore) {}
            }
        }
        return new ArrayList<>(set);
    }

    private List<String> collectLabels(List<Instruction> code) {
        TreeSet<String> set = new TreeSet<>(Comparator.comparingInt(EngineImpl::lIndex));
        boolean hasExit = false;

        for (Instruction ins : code) {
            String lbl = ins.getLabel();
            if (lbl != null && !lbl.isBlank()) set.add(lbl);

            if ("EXIT".equalsIgnoreCase(getStr(ins, "target"))
                    || "EXIT".equalsIgnoreCase(getStr(ins, "targetLabel"))) {
                hasExit = true;
            }
        }
        List<String> out = new ArrayList<>(set);
        if (hasExit) out.add("EXIT");
        return out;
    }

    private static int xIndex(String x) {
        try { return Integer.parseInt(x.substring(1)); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }
    private static int lIndex(String L) {
        try { return Integer.parseInt(L.substring(1)); }
        catch (Exception e) { return Integer.MAX_VALUE; }
    }

    @Override
    public Dtos.ProgramSummary getProgramSummary(int degree) {
        ensureLoaded();
        int d = Math.max(0, degree);
        Program p = current.expandToDegree(d);
        var code = p.getInstructions();
        return Dtos.ProgramSummary.of(
                current.getName(),
                collectInputs(code),
                collectLabels(code),
                toLines(code)
        );
    }
}
