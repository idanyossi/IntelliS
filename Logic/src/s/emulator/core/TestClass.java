package s.emulator.core;
import java.io.File;
import java.util.*;

import static s.emulator.core.BasicInstructions.BasicOp.*;

public class TestClass {

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java s.emulator.core.TestClass <program.xml> [var=value ...]");
            System.err.println("Example: java s.emulator.core.TestClass program.xml x1=4");
            System.exit(1);
        }

        // 1) XML file
        File xmlFile = new File(args[0]);
        if (!xmlFile.isFile()) {
            System.err.println("Program XML not found: " + xmlFile.getAbsolutePath());
            System.exit(2);
        }

        // 2) Program + labels (these are the SAME references passed to the loader)
        List<Instruction> program = new ArrayList<>();
        Map<String, Integer> labels = new HashMap<>();

        // 3) Load from XML -> fills program + labels
        XmlProgramLoader loader = new XmlProgramLoader(program, labels);
        loader.load(xmlFile);

        // 4) Create execution manager and set optional initial variables: var=value
        ExecutionManager em = new ExecutionManager(labels);
        for (int i = 1; i < args.length; i++) {
            String[] kv = args[i].split("=", 2);
            if (kv.length == 2) {
                String var = kv[0].trim();
                try {
                    int val = Integer.parseInt(kv[1].trim());
                    em.set(var, val);
                } catch (NumberFormatException ignored) {
                    System.err.println("Ignoring invalid value for " + var + ": " + kv[1]);
                }
            }
        }

        // 5) Run
        new Interpreter().run(program, em);

        // 6) Output summary
        System.out.println("== Run complete ==");
        System.out.println("Program length: " + program.size());
        System.out.println("Labels: " + labels);
        // Print all variables that were touched (if ExecutionManager exposes them you can iterate; otherwise print common ones)
        for (String v : List.of("x1", "y", "z1")) {
            System.out.println(v + " = " + em.get(v));
        }
    }
}
