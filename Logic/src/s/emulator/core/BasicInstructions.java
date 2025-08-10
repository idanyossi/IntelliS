package s.emulator.core;


public class BasicInstructions implements Instruction {

    public enum BasicOp {INC, DEC, IF_NZ_GOTO, NOOP}

    private final BasicOp op;
    private final String variable;
    private final String label;

    public BasicInstructions(BasicOp op, String variable, String label) {
        this.op = op;
        this.variable = variable;
        this.label = label;
    }

    @Override
    public int execute(ExecutionManager ex,int pc){
      return switch (op){
          case INC -> {ex.increment(variable); yield pc + 1;}
          case DEC -> {ex.decrement(variable); yield pc - 1;}
          case NOOP -> pc + 1;
          case IF_NZ_GOTO -> {
              long val = ex.get(variable);
              yield val != 0 ?  ex.jump(label) : pc + 1;
          }
      };
    }
}
