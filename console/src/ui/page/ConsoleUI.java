package ui.page;

import s.emulator.core.BasicInstructions;
import s.emulator.core.Instruction;
import s.emulator.core.XmlProgramLoader;

import java.io.File;
import java.util.*;
import java.util.regex.Pattern;

public class ConsoleUI {

    private static final List<Instruction> program = new ArrayList<>();
    private static final Map<String, Integer> labels = new LinkedHashMap<>();
    private static final XmlProgramLoader loader = new XmlProgramLoader(program, labels);
    private static Integer version = null;

    public static void main(String[] args) throws Exception {


        Scanner sc = new Scanner(System.in);
        System.out.println("S Console");
        System.out.println("---------");

        while (true) {
            System.out.println("\n1) Load XML file");
            System.out.println("2) Show current program");
            System.out.println("q) Quit");
            System.out.print("> ");

            String choice = sc.nextLine().trim();
            if (choice.equalsIgnoreCase("q")) break;

            switch (choice) {
                case "1" -> doLoad(sc);
                case "2" -> doShow();
                default  -> System.out.println("Unknown choice. Please pick 1 / 2 / q.");
            }
        }

        System.out.println("Bye.");
    }

    private static void doLoad(Scanner sc) throws Exception {
        System.out.print("Enter full XML path (quotes allowed): ");
        String input = sc.nextLine().trim();
        String path = stripQuotes(input);

        loader.load(new File(path));

        if (loader.hasErrors()) {
            System.out.println("File is NOT valid. Errors:");
            loader.getErrors().forEach(e -> System.out.println(String.format(" - %s", e)));
            return;
        }

        version = (version == null) ? 1 : version + 1;
        System.out.println(String.format("File is valid and fully loaded (version %d).", version));
        doShow();
    }

    private static void doShow() {
        if (program.isEmpty()) {
            System.out.println("No valid program is currently loaded.");
            return;
        }

        String programName = (loader.getProgramName() != null) ? loader.getProgramName() : "(unknown)";
        System.out.println(String.format("%n--- Program View ---"));
        System.out.println(String.format("Program name: %s", programName));

        List<String> inputs = collectInputs(program);
        System.out.println(String.format("Inputs: %s", inputs.isEmpty() ? "—" : String.join(", ", inputs)));

        List<String> labelNames = new ArrayList<>(labels.keySet());
        boolean hasExit = labelNames.remove("EXIT");
        if (hasExit) labelNames.add("EXIT");
        System.out.println(String.format("Labels: %s", labelNames.isEmpty() ? "—" : String.join(", ", labelNames)));

        String[] labelByPc = new String[program.size()];
        for (Map.Entry<String, Integer> e : labels.entrySet()) {
            Integer pc = e.getValue();
            if (pc != null && pc >= 0 && pc < labelByPc.length) {
                labelByPc[pc] = e.getKey();
            }
        }

        System.out.println("Instructions:");
        for (int pc = 0; pc < program.size(); pc++) {
            Instruction ins = program.get(pc);
            String kind = "B";
            String labelDef = labelByPc[pc] != null ? labelByPc[pc] : "";
            String line = formatLine(pc, kind, labelDef, ins);
            System.out.println(line);
        }
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2) {
            char f = s.charAt(0), l = s.charAt(s.length() - 1);
            if ((f == '"' && l == '"') || (f == '\'' && l == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }

    private static List<String> collectInputs(List<Instruction> program) {
        List<String> inputs = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        for (Instruction ins : program) {
            if (ins instanceof BasicInstructions bi) {
                String v = bi.getVariable();
                if (isXi(v) && seen.add(v)) {
                    inputs.add(v);
                }
            }
        }
        return inputs;
    }

    private static boolean isXi(String s) {
        if (s == null || s.length() < 2 || s.charAt(0) != 'x') return false;
        for (int i = 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }
        return true;
    }

    private static String formatLine(int pc, String kind, String labelDef, Instruction ins) {
        int number = pc + 1;
        String cmd;
        int cycles;

        if (ins instanceof BasicInstructions bi) {
            BasicInstructions.BasicOp op = bi.getOp();
            String v = bi.getVariable();
            String j = bi.getLabel();

            switch (op) {
                case INC -> {
                    cycles = 1;
                    cmd = String.format("%s <- %s + 1", v, v);
                }
                case DEC -> {
                    cycles = 1;
                    cmd = String.format("%s <- %s - 1", v, v);
                }
                case NOOP -> {
                    cycles = 1;
                    cmd = "NOOP";
                }
                case IF_NZ_GOTO -> {
                    cycles = 2; // per spec/example
                    cmd = String.format("IF %s != 0 GOTO %s", v, (j != null ? j : "?"));
                }
                default -> {
                    cycles = 1;
                    cmd = op.name();
                }
            }
        } else {
            cycles = 1;
            cmd = ins.toString();
        }

        return String.format("#%d (%s) [%-5s] %s (%d)", number, kind,
                (labelDef == null ? "" : labelDef), cmd, cycles);
    }
}
