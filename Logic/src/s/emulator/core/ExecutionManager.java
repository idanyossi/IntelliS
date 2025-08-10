package s.emulator.core;

import java.util.HashMap;
import java.util.Map;

public class ExecutionManager {

    private final Map<String, Long> variables = new HashMap<>();
    private final Map<String,Integer> variableToIndex = new HashMap<>();

    public ExecutionManager(Map<String, Integer> variableToIndex){
        this.variableToIndex.putAll(variableToIndex);
    }

    public long get (String v){
        return variables.getOrDefault(v,0L);
    }
    public void set(String v,long value){
        variables.put(v,value);
    }
    public void increment(String v){
        set(v, get(v)+1);
    }
    public void decrement(String v){
        set(v,get(v)-1);
    }

    // the jump command
    public int jump(String label){
        return variableToIndex.getOrDefault(label,-1);
    }
}
