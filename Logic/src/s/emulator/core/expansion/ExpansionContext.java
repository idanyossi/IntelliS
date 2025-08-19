package s.emulator.core.expansion;

import s.emulator.core.Instruction;
import s.emulator.core.Program;

import java.lang.reflect.Field;

public final class ExpansionContext {

    private int maxX, maxZ, maxLabel;

    public ExpansionContext(int maxX, int maxZ, int maxLabel) {
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.maxLabel = maxLabel;
    }

    public static ExpansionContext fromProgram(Program p) {
        int x = 0, z = 0, l = 0;
        for (Instruction ins : p.getInstructions()) {
            String lbl = ins.getLabel();
            if (lbl != null && lbl.startsWith("L")) {
                try { l = Math.max(l, Integer.parseInt(lbl.substring(1))); } catch (Exception ignore) {}
            }
            for (Field f : ins.getClass().getDeclaredFields()) {
                if (f.getType() != String.class) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(ins);
                    if (!(v instanceof String s)) continue;
                    if (s.startsWith("z")) { try { z = Math.max(z, Integer.parseInt(s.substring(1))); } catch (Exception ignore) {} }
                    if (s.startsWith("x")) { try { x = Math.max(x, Integer.parseInt(s.substring(1))); } catch (Exception ignore) {} }
                } catch (IllegalAccessException ignore) {}
            }
        }
        return new ExpansionContext(x, z, l);
    }

    public String freshZ() { maxZ += 1; return "z" + maxZ; }
    public String freshLabel() { maxLabel += 1; return "L" + maxLabel; }

    public String mapVar(String v) {
        if (v == null || v.isBlank() || "y".equals(v)) return v;
        if (v.startsWith("z")) { try { return "z" + (Integer.parseInt(v.substring(1)) + maxZ); } catch (Exception ignore) {} }
        if (v.startsWith("x")) { try { return "x" + (Integer.parseInt(v.substring(1)) + maxX); } catch (Exception ignore) {} }
        return v;
    }
    public String mapLabel(String L) {
        if (L == null || L.isBlank() || "EXIT".equalsIgnoreCase(L)) return L;
        if (L.startsWith("L")) { try { return "L" + (Integer.parseInt(L.substring(1)) + maxLabel); } catch (Exception ignore) {} }
        return L;
    }

    // current max
    public int maxX() { return maxX; }
    public int maxZ() { return maxZ; }
    public int maxLabel() { return maxLabel; }
}

