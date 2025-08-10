package s.emulator.core;
import java.util.*;

import static s.emulator.core.BasicInstructions.BasicOp.*;

public class TestClass {

    public static void main(String[] args) {
        // 1) Labels: name -> instruction index
        Map<String,Integer> labels = new HashMap<>();
        labels.put("Lloop", 1);
        labels.put("Lbody", 3);

        // 2) Program: copy x1 into y via a loop
        List<Instruction> prog = new ArrayList<>();
        prog.add(new BasicInstructions(INC,       "z1", null));         // 0: z1 = 1 (constant 1)
        prog.add(new BasicInstructions(IF_NZ_GOTO,"x1", "Lbody"));      // 1: if x1!=0 -> body
        prog.add(new BasicInstructions(IF_NZ_GOTO,"z1", "EXIT"));       // 2: else -> EXIT (unconditional via z1==1)
        prog.add(new BasicInstructions(DEC,       "x1", null));         // 3: x1--
        prog.add(new BasicInstructions(INC,       "y",  null));         // 4: y++
        prog.add(new BasicInstructions(IF_NZ_GOTO,"z1", "Lloop"));      // 5: back to loop

        // 3) State + input
        ExecutionManager ex = new ExecutionManager(labels);
        ex.set("x1", 4);                                               // hardcoded input

        // 4) Run
        new Interpreter().run(prog, ex);

        // 5) Check
        System.out.println("y  = " + ex.get("y"));                     // expect 4
        System.out.println("x1 = " + ex.get("x1"));                    // expect 0
        System.out.println("z1 = " + ex.get("z1"));                    // expect 1

    }

}
