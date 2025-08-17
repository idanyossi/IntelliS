package s.emulator.core;

import java.util.List;

public class Interpreter {

    public static long run(ExecutionManager em) {
        List<Instruction> code = em.getProgram().getInstructions();
        while (em.isRunning() && em.getPC() >= 0 && em.getPC() < code.size()) {
            Instruction i = code.get(em.getPC());
            i.execute(em);
        }
        return em.getTotalCycles();
    }
}