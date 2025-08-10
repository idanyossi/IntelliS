package s.emulator.core;

public interface Instruction {

    //execute the command
    int execute(ExecutionManager em, int pc);

    //cost in cycles
     int cycles();
}
