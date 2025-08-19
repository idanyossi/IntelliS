package ui;

import s.emulator.core.Instruction;
import s.emulator.core.Program;
import s.emulator.core.instructions.*;

import java.util.*;
import java.util.function.Function;

public class ProgramPrinter {
    private static final Map<Class<?>, Function<Instruction,String>> RENDERERS = new HashMap<>();
    static {
        // BASIC
        RENDERERS.put(Increase.class, ins -> {
            var i = (Increase) ins; String v = get(i,"var");
            return v + " <- " + v + " + 1";
        });
        RENDERERS.put(Decrease.class, ins -> {
            var i = (Decrease) ins; String v = get(i,"var");
            return v + " <- " + v + " - 1";
        });
        RENDERERS.put(Neutral.class, ins -> {
            var i = (Neutral) ins; String v = get(i,"var");
            return v + " <- " + v;
        });
        RENDERERS.put(JumpNotZero.class, ins -> {
            var i = (JumpNotZero) ins;
            return "IF " + get(i,"var") + " != 0 GOTO " + get(i,"target");
        });

        // SYNTHETIC (non-expanded view)
        RENDERERS.put(ZeroVariable.class, ins -> """
            """.trim()); // filled below; we use reflection since your fields are private
        RENDERERS.put(ConstantAssignment.class, ins -> {
            var i = (ConstantAssignment) ins;
            return get(i,"var") + " <- " + getInt(i,"k");
        });
        RENDERERS.put(Assignment.class, ins -> {
            var i = (Assignment) ins;
            return get(i,"destination") + " <- " + get(i,"source");
        });
        RENDERERS.put(GotoLabel.class, ins -> {
            var i = (GotoLabel) ins;
            return "GOTO " + get(i,"targetLabel");
        });
        RENDERERS.put(JumpZero.class, ins -> {
            var i = (JumpZero) ins;
            return "IF " + get(i,"var") + " = 0 GOTO " + get(i,"targetLabel");
        });
        RENDERERS.put(JumpEqualConstant.class, ins -> {
            var i = (JumpEqualConstant) ins;
            return "IF " + get(i,"var") + " = " + getInt(i,"k") + " GOTO " + get(i,"targetLabel");
        });
        RENDERERS.put(JumpEqualVariable.class, ins -> {
            var i = (JumpEqualVariable) ins;
            return "IF " + get(i,"Vara") + " = " + get(i,"Varb") + " GOTO " + get(i,"targetLabel");
        });
    }

    static {
        // ZeroVariable renderer delayed init to avoid long lambda above
        RENDERERS.put(ZeroVariable.class, ins -> {
            var i = (ZeroVariable) ins;
            return get(i,"var") + " <- 0";
        });
    }

    public static void printProgram(Program program, List<Instruction> code) {
        System.out.println("Program: " + program.getName());
        System.out.println("Inputs: " + String.join(", ", inputsUsed(code)));
        System.out.println("Labels: " + String.join(", ", labelsUsed(code)));
        System.out.println();

        for (int idx = 0; idx < code.size(); idx++) {
            Instruction ins = code.get(idx);
            String kind = ins.isBasic() ? "B" : "S";
            String label = pad5(ins.getLabel());
            String cmd = render(ins);
            String line = String.format("#%d (%s) [%s] %s (%d)", idx + 1, kind, label, cmd, ins.getCycles());
            System.out.println(line);
        }
        System.out.println();
    }

    private static String render(Instruction ins) {
        var f = RENDERERS.get(ins.getClass());
        if (f != null) return f.apply(ins);
        // fallback to class name if something new was added
        return ins.getClass().getSimpleName();
    }

    /** Collect all x_i referenced anywhere in the code (by scanning String fields). */
    private static List<String> inputsUsed(List<Instruction> code) {
        var set = new TreeSet<>(Comparator.comparingInt(ProgramPrinter::xIndex));
        for (var ins : code) {
            for (var f : ins.getClass().getDeclaredFields()) {
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

    /** Collect label names; add EXIT at end if any instruction targets EXIT. */
    private static List<String> labelsUsed(List<Instruction> code) {
        var set = new TreeSet<>(Comparator.comparingInt(ProgramPrinter::lIndex));
        boolean hasExit = false;

        for (var ins : code) {
            String L = ins.getLabel();
            if (L != null && !L.isBlank()) set.add(L);

            if (ins instanceof JumpNotZero jnz && "EXIT".equalsIgnoreCase(get(jnz,"target"))) hasExit = true;
            if (ins instanceof GotoLabel gl && "EXIT".equalsIgnoreCase(get(gl,"targetLabel"))) hasExit = true;
            if (ins instanceof JumpZero jz && "EXIT".equalsIgnoreCase(get(jz,"targetLabel"))) hasExit = true;
            if (ins instanceof JumpEqualConstant jec && "EXIT".equalsIgnoreCase(get(jec,"targetLabel"))) hasExit = true;
            if (ins instanceof JumpEqualVariable jev && "EXIT".equalsIgnoreCase(get(jev,"targetLabel"))) hasExit = true;
        }
        var out = new ArrayList<>(set);
        if (hasExit) out.add("EXIT");
        return out;
    }

    private static String pad5(String L) {
        if (L == null || L.isBlank()) return "     ";
        return String.format("%-5s", L);
    }

    // reflect helpers (keeps UI decoupled from core field visibility)
    private static String get(Object o, String field){
        try { var f = o.getClass().getDeclaredField(field); f.setAccessible(true); return (String) f.get(o); }
        catch (Exception e) { return "?"; }
    }
    private static int getInt(Object o, String field){
        try { var f = o.getClass().getDeclaredField(field); f.setAccessible(true); return f.getInt(o); }
        catch (Exception e) { return -1; }
    }

    public static String formatOne(int number, Instruction ins) {
        String kind = ins.isBasic() ? "B" : "S";
        String label = ins.getLabel()==null || ins.getLabel().isBlank() ? "     " : String.format("%-5s", ins.getLabel());
        String cmd = render(ins); // uses the existing RENDERERS map you already have
        return String.format("#%d (%s) [%s] %s (%d)", number, kind, label, cmd, ins.getCycles());
    }

    private static int xIndex(String x){ try{ return Integer.parseInt(x.substring(1)); } catch(Exception e){ return Integer.MAX_VALUE; } }
    private static int lIndex(String L){ try{ return Integer.parseInt(L.substring(1)); } catch(Exception e){ return Integer.MAX_VALUE; } }
}
