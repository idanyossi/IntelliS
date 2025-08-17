package s.emulator.core;

import java.util.List;

public interface Instruction {

    String getLabel();

    int getCycles();

    void execute(ExecutionManager em);

    default boolean isBasic() { return true; }

    default List<Instruction> expand(ExpansionContext ctx) { return List.of(this); }



}
