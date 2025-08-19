package s.emulator.core;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import s.emulator.core.instructions.JumpNotZero;
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
            final String opcode   = upper(reqAttr(si.getName(), "S-Instruction/@name")); // e.g., INCREASE, ZERO_VARIABLE
            final String typeHint = safeTrim(si.getType(), null);                         // "basic"/"synthetic" or null
            final String variable = safeTrim(si.getSVariable(), null);
            final String label    = safeTrim(si.getSLabel(), null);
            final Map<String,String> args = toArgMap(si.getSInstructionArguments());

            Class<?> cls = resolveInstructionClass(opcode, typeHint);

            var ctor = cls.getDeclaredConstructor();
            ctor.setAccessible(true);
            Instruction proto = (Instruction) ctor.newInstance();

            Instruction insn = proto.buildFromXml(label, variable, args);

            code.add(insn);
        }

        return new Program(programName, code);
    }

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

    private static Map<String,String> toArgMap(SInstructionArguments args) {
        Map<String,String> map = new HashMap<>();
        if (args != null && args.getSInstructionArgument() != null) {
            for (SInstructionArgument a : args.getSInstructionArgument()) {
                if (a.getName() != null) {
                    map.put(a.getName(), a.getValue() == null ? "" : a.getValue().trim());
                }
            }
        }
        return map;
    }

    /** Resolve instruction class by XML name; prefer package by type hint. */
    private static Class<?> resolveInstructionClass(String opcode, String typeHint) {
        String simple = toClassSimpleName(opcode); // e.g., JUMP_EQUAL_CONSTANT -> JumpEqualConstant
        List<String> pkgs = new ArrayList<>();
        if ("basic".equalsIgnoreCase(typeHint)) {
            pkgs.add("s.emulator.core.instructions");
            pkgs.add("s.emulator.synth");
        } else if ("synthetic".equalsIgnoreCase(typeHint)) {
            pkgs.add("s.emulator.synth");
            pkgs.add("s.emulator.core.instructions");
        } else {
            pkgs.add("s.emulator.synth");
            pkgs.add("s.emulator.core.instructions");
        }
        for (String pkg : pkgs) {
            try { return Class.forName(pkg + "." + simple); }
            catch (ClassNotFoundException ignore) {}
        }
        throw new IllegalArgumentException("No instruction class for opcode " + opcode +
                " (expected " + simple + " under " + pkgs + ")");
    }

    private static String toClassSimpleName(String opcode) {
        String[] parts = opcode.toLowerCase(Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty()) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        return sb.toString();
    }

}