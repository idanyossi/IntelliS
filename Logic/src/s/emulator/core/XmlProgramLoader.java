package s.emulator.core;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import s.emulator.core.instructions.Decrease;
import s.emulator.core.instructions.Increase;
import s.emulator.core.instructions.JumpNotZero;
import s.emulator.core.instructions.NoOp;
import s.emulator.jaxb.*;

import java.io.File;
import java.util.*;

public class XmlProgramLoader {
    public Program load(File xmlFile) throws Exception {
        requireXml(xmlFile);

        JAXBContext ctx = JAXBContext.newInstance(SProgram.class.getPackageName());
        Unmarshaller um = ctx.createUnmarshaller();
        Object root = um.unmarshal(xmlFile);
        if (!(root instanceof SProgram sprog)) {
            throw new IllegalArgumentException("Root element is not <S-Program>.");
        }

        final String programName = safeTrim(sprog.getName(), "Unnamed");

        SInstructions sIns = sprog.getSInstructions();
        if (sIns == null || sIns.getSInstruction() == null) {
            throw new IllegalArgumentException("Missing <S-Instructions> or it is empty.");
        }

        List<SInstruction> raw = sIns.getSInstruction();
        List<Instruction> code = new ArrayList<>(raw.size());

        for (SInstruction si : raw) {
            final String type = upper(safeTrim(si.getType(), "basic"));
            final String name = upper(reqAttr(si.getName(), "S-Instruction/@name"));
            final String var  = safeTrim(si.getSVariable(), null);
            final String lbl  = safeTrim(si.getSLabel(), null);

            // Only "basic" for this milestone; ignore/validate otherwise if you want.
            if (!"BASIC".equals(type)) {
                throw new IllegalArgumentException("Only basic instructions are supported at this stage. Found type=" + type);
            }

            Instruction insn = switch (name) {
                case "INCREASE" -> {
                    ensureVar("INCREASE", var);
                    yield new Increase(lbl, var);
                }
                case "DECREASE" -> {
                    ensureVar("DECREASE", var);
                    yield new Decrease(lbl, var);
                }
                case "NEUTRAL" -> {
                    // Spec says NO-OP is V <- V, so we still expect a variable token.
                    ensureVar("NEUTRAL", var);
                    yield new NoOp(lbl, var);
                }
                case "JUMP_NOT_ZERO" -> {
                    ensureVar("JUMP_NOT_ZERO", var);
                    String target = findArg(si.getSInstructionArguments(), "JNZLabel");
                    if (target == null || target.isBlank()) {
                        throw new IllegalArgumentException("JUMP_NOT_ZERO requires <S-Instruction-Argument name=\"JNZLabel\" value=\"...\"/>.");
                    }
                    yield new JumpNotZero(lbl, var, target.trim());
                }
                default -> throw new IllegalArgumentException("Unknown basic instruction name: " + name);
            };

            code.add(insn);
        }

        // Eager validation (friendly error if jump references missing label)
        validateJnzTargets(code);

        return new Program(programName, code);
    }

    // ---------- helpers ----------

    private static void requireXml(File f) {
        if (f == null) throw new IllegalArgumentException("File is null.");
        if (!f.exists()) throw new IllegalArgumentException("File not found: " + f.getAbsolutePath());
        String n = f.getName().toLowerCase(Locale.ROOT);
        if (!n.endsWith(".xml")) throw new IllegalArgumentException("Expected a .xml file, got: " + f.getName());
    }

    private static String safeTrim(String s, String fallback) {
        if (s == null) return fallback;
        String t = s.trim();
        return t.isEmpty() ? fallback : t;
    }

    private static String upper(String s) {
        return s == null ? null : s.toUpperCase(Locale.ROOT);
    }

    private static String reqAttr(String s, String where) {
        if (s == null || s.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing required attribute/text at " + where);
        }
        return s.trim();
    }

    private static void ensureVar(String op, String var) {
        if (var == null || var.isBlank()) {
            throw new IllegalArgumentException(op + " requires <S-Variable>.");
        }
    }

    /** Extract specific argument (by @name) from <S-Instruction-Arguments>. */
    private static String findArg(SInstructionArguments args, String wantedName) {
        if (args == null || args.getSInstructionArgument() == null) return null;
        for (SInstructionArgument a : args.getSInstructionArgument()) {
            if (wantedName.equals(a.getName())) {
                String val = a.getValue();
                return val == null ? null : val.trim();
            }
        }
        return null;
    }

    /** Fail-fast if any JNZ (non-EXIT) points to a label that does not exist. */
    private static void validateJnzTargets(List<Instruction> code) {
        Set<String> labels = new HashSet<>();
        for (Instruction ins : code) {
            String lbl = ins.getLabel();
            if (lbl != null && !lbl.isBlank()) labels.add(lbl.trim());
        }
        for (Instruction ins : code) {
            if (ins instanceof JumpNotZero jnz) {
                String tgt = getTargetLabel(jnz);
                if (tgt != null && !"EXIT".equalsIgnoreCase(tgt) && !labels.contains(tgt)) {
                    throw new IllegalArgumentException("Unknown label referenced by JUMP_NOT_ZERO: " + tgt);
                }
            }
        }
    }

    // Prefer: add a real getter in JumpNotZero, e.g., public String getTargetLabel().
    private static String getTargetLabel(JumpNotZero jnz) {
        try {
            var f = JumpNotZero.class.getDeclaredField("target");
            f.setAccessible(true);
            Object v = f.get(jnz);
            return v == null ? null : v.toString();
        } catch (Exception ignore) {
            return null; // skip if not accessible
        }
    }

}