package s.emulator.core;

import java.util.List;

public class Interpreter {

    public void run(List<Instruction> program, ExecutionManager ex) {
        int pc = 0;
        while (pc >= 0 && pc < program.size()) {
            pc = program.get(pc).execute(ex, pc);
        }
    }
}