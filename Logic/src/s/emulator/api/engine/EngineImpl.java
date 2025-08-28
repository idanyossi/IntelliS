package s.emulator.api.engine;

import s.emulator.api.Engine;
import s.emulator.api.dto.Dtos;
import s.emulator.core.*;
import s.emulator.core.expansion.ExpansionContext;
import s.emulator.core.instructions.Decrease;
import s.emulator.core.instructions.Increase;
import s.emulator.core.instructions.JumpNotZero;
import s.emulator.core.instructions.Neutral;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class EngineImpl implements Engine {
    private Program current;
    private final List<Dtos.RunHistoryEntry> history = new ArrayList<>();
    private byte[] lastXmlBytes;


    @Override
    public void loadProgram(File xml) throws Exception {
        current = new XmlProgramLoader().load(xml);
        lastXmlBytes = Files.readAllBytes(xml.toPath());
        validateProgram(current);
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

        List<Dtos.NameValue> inputsForHistory =
                (inputsByName == null) ? List.of() :
                        inputsByName.entrySet().stream()
                                .filter(e -> e.getKey() != null && e.getKey().matches("x\\d+"))
                                .sorted(Comparator.comparingInt(e -> xIndex(e.getKey())))
                                .map(e -> Dtos.NameValue.of(
                                        e.getKey(),
                                        Math.max(0, e.getValue() == null ? 0 : e.getValue())))
                                .collect(Collectors.toList());
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
    @Override
    public Dtos.ChainSummary getProgramChainSummary(int degree) {
        ensureLoaded();
        final int D = Math.max(0, degree);

        // Build layers 0..D and, for each step k (1..D), a child->parent map from degree k to k-1
        List<List<Instruction>> layers = new ArrayList<>(D + 1);
        List<Map<Instruction, Instruction>> parentAt = new ArrayList<>(D + 1);

        // degree 0 (original)
        List<Instruction> layer0 = current.getInstructions();
        layers.add(layer0);
        parentAt.add(null); // no parent map for degree 0

        ExpansionContext ctx = ExpansionContext.fromProgram(current);
        for (int k = 1; k <= D; k++) {
            List<Instruction> prev = layers.get(k - 1);
            List<Instruction> next = new ArrayList<>();
            Map<Instruction, Instruction> map = new IdentityHashMap<>(); // child@k -> parent@(k-1)

            for (Instruction p : prev) {
                if (p.isBasic()) {
                    // pass-through: same object, not "derived" this step
                    next.add(p);
                    // no entry in map => no derivation at this step
                } else {
                    for (Instruction c : p.expand(ctx)) {
                        next.add(c);
                        map.put(c, p);
                    }
                }
            }
            layers.add(next);
            parentAt.add(map);
        }

        // Index maps per degree: instruction -> line number (0-based)
        List<Map<Instruction, Integer>> idxAt = new ArrayList<>(layers.size());
        for (List<Instruction> layer : layers) {
            Map<Instruction, Integer> m = new IdentityHashMap<>();
            for (int i = 0; i < layer.size(); i++) m.put(layer.get(i), i);
            idxAt.add(m);
        }

        // Build ChainLine list for final degree D
        List<Dtos.ChainLine> out = new ArrayList<>();
        List<Instruction> finalLayer = layers.get(D);
        for (int i = 0; i < finalLayer.size(); i++) {
            Instruction cur = finalLayer.get(i);
            Dtos.InstructionLine self = toLine(i + 1, cur);

            List<Dtos.InstructionLine> parents = new ArrayList<>();
            Instruction walk = cur;

            // Walk up D..1; only add when a real derivation happened at that step
            for (int k = D; k >= 1; k--) {
                Map<Instruction, Instruction> map = parentAt.get(k);
                Instruction parent = map.get(walk); // null => pass-through this step
                if (parent != null) {
                    int parentIdx = idxAt.get(k - 1).get(parent);
                    parents.add(toLine(parentIdx + 1, parent));
                    walk = parent; // keep climbing
                }
                // if parent == null: keep 'walk' as-is and continue climbing further back
            }
            out.add(Dtos.ChainLine.of(self, parents));
        }

        return Dtos.ChainSummary.of(current.getName(), D, out);
    }

    @Override
    public void saveSnapshot(File basePathNoExt) throws Exception {
        ensureLoaded();
        if (lastXmlBytes == null || lastXmlBytes.length == 0) {
            throw new IllegalStateException("No XML cached for current program; load an XML first.");
        }
        SavedState snap = new SavedState();
        snap.programName = current.getName();
        snap.xmlContent  = lastXmlBytes;
        snap.history     = toPlainHistory(history);

        File out = withExt(basePathNoExt, ".ser");
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(out))) {
            oos.writeObject(snap);
        }
    }

    @Override
    public void loadSnapshot(File basePathNoExt) throws Exception {
        File in = withExt(basePathNoExt, ".ser");
        if (!in.exists()) throw new FileNotFoundException("Snapshot not found: " + in.getAbsolutePath());

        SavedState snap;
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(in))) {
            snap = (SavedState) ois.readObject();
        }

        // Recreate program from XML bytes (no need to change Program classes)
        File tmp = File.createTempFile("prog-", ".xml");
        Files.write(tmp.toPath(), snap.xmlContent);
        try {
            current = new XmlProgramLoader().load(tmp);
        } finally {
            tmp.delete();
        }

        this.lastXmlBytes = snap.xmlContent;
        this.history.clear();
        this.history.addAll(fromPlainHistory(snap.history));
    }

    private static File withExt(File base, String ext) {
        String p = base.getPath();
        if (p.endsWith(ext)) return base;
        return new File(p + ext);
    }

    /* ---------- Serializable snapshot DTOs ---------- */
    private static final class SavedState implements Serializable {
        private static final long serialVersionUID = 1L;
        String programName;
        byte[] xmlContent; // original XML bytes
        List<RunHistoryPlain> history;
    }

    private static final class RunHistoryPlain implements Serializable {
        private static final long serialVersionUID = 1L;
        int runNo;
        int degree;
        List<NameValuePlain> inputs;
        int y;
        long cycles;
    }

    private static final class NameValuePlain implements Serializable {
        private static final long serialVersionUID = 1L;
        String name;
        int value;
    }

    /* ---------- Mappers between Dtos & plain ---------- */
    private static List<RunHistoryPlain> toPlainHistory(List<Dtos.RunHistoryEntry> hist) {
        List<RunHistoryPlain> out = new ArrayList<>(hist.size());
        for (Dtos.RunHistoryEntry e : hist) {
            RunHistoryPlain p = new RunHistoryPlain();
            p.runNo = e.getRunNo();
            p.degree = e.getDegree();
            p.y = e.getY();
            p.cycles = e.getCycles();
            p.inputs = new ArrayList<>();
            for (Dtos.NameValue nv : e.getInputs()) {
                NameValuePlain np = new NameValuePlain();
                np.name = nv.getName();
                np.value = nv.getValue();
                p.inputs.add(np);
            }
            out.add(p);
        }
        return out;
    }

    private static List<Dtos.RunHistoryEntry> fromPlainHistory(List<RunHistoryPlain> src) {
        if (src == null) return List.of();
        List<Dtos.RunHistoryEntry> out = new ArrayList<>(src.size());
        for (RunHistoryPlain p : src) {
            List<Dtos.NameValue> ins = new ArrayList<>();
            if (p.inputs != null) {
                for (NameValuePlain np : p.inputs) {
                    ins.add(Dtos.NameValue.of(np.name, np.value));
                }
            }
            out.add(Dtos.RunHistoryEntry.of(p.runNo, p.degree, ins, p.y, p.cycles));
        }
        return out;
    }

    private static boolean isBlank(String s) { return s == null || s.isBlank(); }

    private static boolean isNumeric(String s) {
        return s != null && s.matches("-?\\d+");
    }
    private static boolean isExit(String s) {
        return s != null && s.equalsIgnoreCase("EXIT");
    }
    private static boolean isLabelToken(String s) {
        return s != null && s.matches("L\\d+");
    }
    private static boolean isVarToken(String s) {
        // valid: y   OR   x<number>   OR   z<number>
        return s != null && (s.equals("y") || s.matches("x\\d+") || s.matches("z\\d+"));
    }

    /** Extract any String-like “label references” from an instruction by reflection.
     * Heuristic: any String field whose value looks like L<digits> or EXIT is considered a label reference.
     */
    private static List<String> findReferencedLabels(Instruction ins) {
        List<String> out = new ArrayList<>();
        for (Field f : ins.getClass().getDeclaredFields()) {
            if (f.getType() != String.class) continue;
            f.setAccessible(true);
            try {
                Object v = f.get(ins);
                if (!(v instanceof String s) || isBlank(s)) continue;
                if (isLabelToken(s) || isExit(s)) out.add(s);
            } catch (Exception ignore) {}
        }
        return out;
    }

    /** Extract any String-like “variables” from an instruction by reflection.
     * Heuristic: consider String fields that are NOT labels/EXIT/numerics; the valid ones must be y/x#/z#.
     */
    private static List<String> findVariables(Instruction ins) {
        List<String> out = new ArrayList<>();
        for (Field f : ins.getClass().getDeclaredFields()) {
            if (f.getType() != String.class) continue;
            f.setAccessible(true);
            try {
                Object v = f.get(ins);
                if (!(v instanceof String s) || isBlank(s)) continue;
                if (isExit(s) || isLabelToken(s) || isNumeric(s)) continue; // not a variable
                out.add(s);
            } catch (Exception ignore) {}
        }
        return out;
    }

    private void validateProgram(Program program) {
        List<Instruction> code = program.getInstructions();

        // 1) Collect declared labels & validate their format
        final Set<String> declaredLabels = new HashSet<>();
        for (int i = 0; i < code.size(); i++) {
            Instruction ins = code.get(i);
            String lbl = ins.getLabel();
            if (!isBlank(lbl)) {
                if (!isLabelToken(lbl)) {
                    String msg = String.format(
                            "Invalid label format at line #%d: \"%s\". Labels must be L<digits> (e.g., L7).",
                            i + 1, lbl);
                    throw new IllegalArgumentException(msg);
                }
                declaredLabels.add(lbl);
            }
        }

        // 2) Validate variable tokens across all String fields (excluding labels/EXIT/numerics)
        for (int i = 0; i < code.size(); i++) {
            Instruction ins = code.get(i);
            List<String> vars = findVariables(ins);
            for (String v : vars) {
                if (!isVarToken(v)) {
                    String msg = String.format(
                            "Invalid variable \"%s\" at line #%d. Valid names: y, x<digits>, z<digits>.",
                            v, i + 1);
                    throw new IllegalArgumentException(msg);
                }
            }
        }

        // 3) Validate that every referenced label exists (unless it is EXIT)
        for (int i = 0; i < code.size(); i++) {
            Instruction ins = code.get(i);
            for (String target : findReferencedLabels(ins)) {
                if (isExit(target)) continue; // EXIT is always allowed
                if (!declaredLabels.contains(target)) {
                    String msg = String.format(
                            "Jump to unknown label at line #%d: \"%s\" is not declared in this program.",
                            i + 1, target);
                    throw new IllegalArgumentException(msg);
                }
            }
        }
    }
}
