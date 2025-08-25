package s.emulator.api.dto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Dtos {
    private Dtos() {}

    public static final class ProgramSummary {
        private final String programName;
        private final List<String> inputsUsed;
        private final List<String> labelsUsed;
        private final List<InstructionLine> instructions;

        private ProgramSummary(String programName,
                               List<String> inputsUsed,
                               List<String> labelsUsed,
                               List<InstructionLine> instructions) {
            this.programName  = programName;
            this.inputsUsed   = List.copyOf(inputsUsed);
            this.labelsUsed   = List.copyOf(labelsUsed);
            this.instructions = List.copyOf(instructions);
        }
        public static ProgramSummary of(String programName,
                                        List<String> inputsUsed,
                                        List<String> labelsUsed,
                                        List<InstructionLine> instructions) {
            return new ProgramSummary(programName, inputsUsed, labelsUsed, instructions);
        }
        public String getProgramName() { return programName; }
        public List<String> getInputsUsed() { return inputsUsed; }
        public List<String> getLabelsUsed() { return labelsUsed; }
        public List<InstructionLine> getInstructions() { return instructions; }
    }

    public static final class InstructionLine {
        private final int lineNumber;
        private final boolean basic;
        private final String label;
        private final String display;
        private final int cycles;

        private InstructionLine(int lineNumber, boolean basic, String label, String display, int cycles) {
            this.lineNumber = lineNumber;
            this.basic = basic;
            this.label = label;
            this.display = display;
            this.cycles = cycles;
        }
        public static InstructionLine of(int lineNumber, boolean basic, String label, String display, int cycles) {
            return new InstructionLine(lineNumber, basic, label, display, cycles);
        }
        public int getLineNumber() { return lineNumber; }
        public boolean isBasic() { return basic; }
        public String getLabel() { return label; }
        public String getDisplay() { return display; }
        public int getCycles() { return cycles; }
    }
    
    public static final class ExpansionPreview {
        private final int degree;
        private final List<ExpansionRow> rows;

        private ExpansionPreview(int degree, List<ExpansionRow> rows) {
            this.degree = degree;
            this.rows = List.copyOf(rows);
        }
        public static ExpansionPreview of(int degree, List<ExpansionRow> rows) {
            return new ExpansionPreview(degree, rows);
        }
        public int getDegree() { return degree; }
        public List<ExpansionRow> getRows() { return rows; }
    }

    public static final class ExpansionRow {
        private final InstructionLine origin;
        private final List<InstructionLine> tail;

        private ExpansionRow(InstructionLine origin, List<InstructionLine> tail) {
            this.origin = origin;
            this.tail = List.copyOf(tail);
        }
        public static ExpansionRow of(InstructionLine origin, List<InstructionLine> tail) {
            return new ExpansionRow(origin, tail);
        }
        public InstructionLine getOrigin() { return origin; }
        public List<InstructionLine> getTail() { return tail; }
    }

    public static final class RunResult {
        private final String programName;
        private final int degree;
        private final int y;
        private final long cycles;
        private final List<NameValue> variables; // ordered y,x*,z*

        private RunResult(String programName, int degree, int y, long cycles, List<NameValue> variables) {
            this.programName = programName;
            this.degree = degree;
            this.y = y;
            this.cycles = cycles;
            this.variables = List.copyOf(variables);
        }
        public static RunResult of(String programName, int degree, int y, long cycles, List<NameValue> variables) {
            return new RunResult(programName, degree, y, cycles, variables);
        }
        public String getProgramName() { return programName; }
        public int getDegree() { return degree; }
        public int getY() { return y; }
        public long getCycles() { return cycles; }
        public List<NameValue> getVariables() { return variables; }
    }

    public static final class RunHistoryEntry {
        private final int runNo;
        private final int degree;
        private final List<NameValue> inputs; // xN,value
        private final int y;
        private final long cycles;

        private RunHistoryEntry(int runNo, int degree, List<NameValue> inputs, int y, long cycles) {
            this.runNo = runNo;
            this.degree = degree;
            this.inputs = List.copyOf(inputs);
            this.y = y;
            this.cycles = cycles;
        }
        public static RunHistoryEntry of(int runNo, int degree, List<NameValue> inputs, int y, long cycles) {
            return new RunHistoryEntry(runNo, degree, inputs, y, cycles);
        }
        public int getRunNo() { return runNo; }
        public int getDegree() { return degree; }
        public List<NameValue> getInputs() { return inputs; }
        public int getY() { return y; }
        public long getCycles() { return cycles; }
    }

    public static final class NameValue {
        private final String name;
        private final int value;

        private NameValue(String name, int value) {
            this.name = name;
            this.value = value;
        }
        public static NameValue of(String name, int value) {
            return new NameValue(name, value);
        }
        public String getName() { return name; }
        public int getValue() { return value; }
    }

    public static <T> List<T> listOf() { return Collections.emptyList(); }
    public static <T> List<T> listOf(T t) { return List.of(t); }
    public static <T> List<T> listOf(T t1, T t2) { return List.of(t1,t2); }
    public static <T> List<T> mutable() { return new ArrayList<>(); }
}
