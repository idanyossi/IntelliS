package s.emulator.core;
import java.io.File;
import java.util.*;

public class TestClass {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: java ... TestClass <xmlPath> [x1=4,x2=7,...]");
            System.exit(1);
        }
        File xml = new File(args[0]);

        XmlProgramLoader loader = new XmlProgramLoader();
        Program program = loader.load(xml);

        ExecutionManager em = new ExecutionManager(program);

        // Optional: parse k=v inputs after path (x*, z*, y allowed; absent => 0)
        if (args.length >= 2) {
            for (int i = 1; i < args.length; i++) {
                String[] kv = args[i].split("=", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim();
                    int v = Integer.parseInt(kv[1].trim());
                    em.setVar(k, v);
                }
            }
        }

        long cycles = Interpreter.run(em);

        System.out.println("Program: " + program.getName());
        System.out.println("Cycles: " + cycles);

        // Print variables: y, xs asc, zs asc (already ordered by snapshotVars)
        for (Map.Entry<String,Integer> e : em.snapshotVars().entrySet()) {
            System.out.println(e.getKey() + " = " + e.getValue());
        }
    }
}
