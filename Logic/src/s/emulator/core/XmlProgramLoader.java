package s.emulator.core;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import s.emulator.jaxb.SInstruction;
import s.emulator.jaxb.SInstructionArgument;
import s.emulator.jaxb.SInstructions;
import s.emulator.jaxb.SProgram;

import java.io.File;
import java.util.*;

import static s.emulator.core.BasicInstructions.BasicOp.*;

public class XmlProgramLoader {
    public final List<Instruction> program;
    public final Map<String,Integer> labels;
    public List<String> errors = new ArrayList<>();

    private String programName;

    public XmlProgramLoader(List<Instruction> program, Map<String,Integer> labels) {
        this.program = program;
        this.labels = labels;
    }

    public void load(File xmlFile) throws Exception{

        errors.clear();

        if (!basicFileChecks(xmlFile)) return;

        SProgram sProgram = parse(xmlFile);
        if (sProgram == null) return;

        List<SInstruction> sList = getSInstructionList(sProgram);
        if (sList == null) return;

        List<Instruction> tmpProgram = new ArrayList<>(sList.size());
        Map<String,Integer> tmpLabels = new LinkedHashMap<>();
        Map<String,Integer> firstDef  = new HashMap<>();

        buildTemp(sList, tmpProgram, tmpLabels, firstDef);


        validateCommands(tmpProgram);
        validateLabels(tmpProgram, tmpLabels, firstDef);

        if (!errors.isEmpty()) return;

        // Commit
        program.clear(); program.addAll(tmpProgram);
        labels.clear();  labels.putAll(tmpLabels);
        programName = sProgram.getName();
    }

    public boolean hasErrors() { return !errors.isEmpty(); }
    public List<String> getErrors() { return Collections.unmodifiableList(errors); }
    public String getProgramName() { return programName; }

    private boolean basicFileChecks(File xmlFile) {
        if (xmlFile == null) { errors.add("file not supplied "); return false; }
        if (!xmlFile.isFile()) { errors.add("file not found " + xmlFile.getAbsolutePath()); return false; }
        if (!xmlFile.getName().toLowerCase(Locale.ROOT).endsWith(".xml")) {
            errors.add("must provide a file with .xml"); return false;
        }
        return true;
    }

    private SProgram parse(File xmlFile) {
        try {
            JAXBContext ctx = JAXBContext.newInstance(SProgram.class);
            Unmarshaller um = ctx.createUnmarshaller();
            return (SProgram) um.unmarshal(xmlFile);
        } catch (Exception e) {
            String msg = (e.getCause() != null && e.getCause().getMessage() != null)
                    ? e.getCause().getMessage() : String.valueOf(e.getMessage());
            errors.add("xml parsing error " + msg);
            return null;
        }
    }

    private List<SInstruction> getSInstructionList(SProgram sProgram) {
        if (sProgram == null || sProgram.getSInstructions() == null) {
            errors.add("no instructions block");
            return null;
        }
        List<SInstruction> list = sProgram.getSInstructions().getSInstruction();
        if (list == null || list.isEmpty()) {
            errors.add("no commands in the file");
            return null;
        }
        return list;
    }

    private void buildTemp(List<SInstruction> sList, List<Instruction> tmpProgram, Map<String,Integer> tmpLabels, Map<String,Integer> firstDef) {
        int pc = 0;
        for (SInstruction si : sList) {
            BasicInstructions.BasicOp op;
            try {
                op = mapOp(si.getName());
            } catch (IllegalArgumentException iae) {
                errors.add("unsupported command in line: #" + (pc + 1) + ": " + si.getName());
                op = NOOP;
            }

            String var = si.getSVariable();

            // Label definition
            String labelDef = si.getSLabel();
            if (labelDef != null && !labelDef.isEmpty()) {
                Integer prev = firstDef.putIfAbsent(labelDef, pc);
                if (prev == null) {
                    tmpLabels.put(labelDef, pc);
                } else if (!Objects.equals(prev, pc)) {
                    tmpLabels.put(labelDef, pc);
                }
            }

            String jumpTarget = (op == IF_NZ_GOTO) ? extractJumpTarget(si) : null;

            tmpProgram.add(new BasicInstructions(op, var, jumpTarget));
            pc++;
        }
    }
    private void validateCommands(List<Instruction> tmpProgram) {
        for (int i = 0; i < tmpProgram.size(); i++) {
            Instruction ins = tmpProgram.get(i);
            if (ins instanceof BasicInstructions bi) {
                if (bi.getOp() == IF_NZ_GOTO) {
                    String target = bi.getLabel();
                    if (target == null || target.isEmpty()) {
                        errors.add("jump with no destination label #" + (i + 1));
                    }
                }
            }
        }
    }

    private void validateLabels(List<Instruction> tmpProgram, Map<String,Integer> tmpLabels, Map<String,Integer> firstDef) {
        for (Map.Entry<String,Integer> e : tmpLabels.entrySet()) {
            String name = e.getKey();
            Integer pc = e.getValue();
            Integer first = firstDef.get(name);
            if (first != null && !Objects.equals(first, pc)) {
                errors.add("Duplicate label '" + name + "' (lines #" + (first + 1) + " and #" + (pc + 1) + ")");
            }
        }

        for (int i = 0; i < tmpProgram.size(); i++) {
            Instruction ins = tmpProgram.get(i);
            if (ins instanceof BasicInstructions bi) {
                if (bi.getOp() == IF_NZ_GOTO) {
                    String t = bi.getLabel();
                    if (t != null && !t.isEmpty() && !"EXIT".equals(t) && !tmpLabels.containsKey(t)) {
                        errors.add("jump to label that doesnt exist" + t + "'# line: " + (i + 1));
                    }
                }
            }
        }

        for (Map.Entry<String,Integer> e : tmpLabels.entrySet()) {
            Integer p = e.getValue();
            if (p == null || p < 0 || p >= tmpProgram.size()) {
                errors.add("label'" + e.getKey() + "' points to an illegal line:  " + p);
            }
        }
    }

    private static String extractJumpTarget(SInstruction si) {
        if (si.getSInstructionArguments() == null
                || si.getSInstructionArguments().getSInstructionArgument() == null
                || si.getSInstructionArguments().getSInstructionArgument().isEmpty()) {
            return null;
        }
        Object first = si.getSInstructionArguments().getSInstructionArgument().getFirst();
        if (first instanceof String) return (String) first;
        if (first != null) return ((SInstructionArgument) first).getValue();
        return (first != null) ? String.valueOf(first) : null;
    }

    private static BasicInstructions.BasicOp mapOp(String xmlName) {
        if (xmlName == null) throw new IllegalArgumentException("no name for the file.");
        switch (xmlName) {
            case "INCREASE":      return INC;
            case "DECREASE":      return DEC;
            case "NEUTRAL":       return NOOP;
            case "JUMP_NOT_ZERO": return IF_NZ_GOTO;
            default: throw new IllegalArgumentException("unsupported command " + xmlName);
        }
    }




}