package s.emulator.core;

import java.util.List;
import java.util.Map;

public interface Instruction {

    String getLabel();

    int getCycles();

    void execute(ExecutionManager em);

    Instruction buildFromXml(String label, String variable, Map<String,String> args);

    default boolean isBasic() { return true; }

    default List<Instruction> expand(ExpansionContext ctx) { return List.of(this); }



}
