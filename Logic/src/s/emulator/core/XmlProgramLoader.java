package s.emulator.core;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import s.emulator.jaxb.SInstruction;
import s.emulator.jaxb.SInstructionArgument;   // FIX: import to read jump target
import s.emulator.jaxb.SInstructions;
import s.emulator.jaxb.SProgram;

import java.io.File;
import java.util.List;
import java.util.Map;

import static s.emulator.core.BasicInstructions.BasicOp.*;

public class XmlProgramLoader {
    public final List<Instruction> program;
    public final Map<String,Integer> labels;

    public XmlProgramLoader(List<Instruction> program, Map<String,Integer> labels) {
        this.program = program;
        this.labels = labels;
    }

    public void load(File xmlFile) throws Exception{
        JAXBContext context = JAXBContext.newInstance(SProgram.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        SProgram sProgram = (SProgram) unmarshaller.unmarshal(xmlFile);

        labels.clear();
        program.clear();

        SInstructions sins = (sProgram != null) ? sProgram.getSInstructions() : null;
        if (sins == null || sins.getSInstruction() == null || sins.getSInstruction().isEmpty()) {
            return;
        }

        int pc = 0;
        for (SInstruction si : sins.getSInstruction()){
            String xmlOper = si.getName();
            BasicInstructions.BasicOp op = mapOp(xmlOper);

            String var = si.getSVariable();

            String labelDef = si.getSLabel();
            if (labelDef != null && !labelDef.isEmpty()) {
                labels.put(labelDef, pc);
            }

            String jumpTarget = null;
            if (op == IF_NZ_GOTO
                    && si.getSInstructionArguments() != null
                    && si.getSInstructionArguments().getSInstructionArgument() != null
                    && !si.getSInstructionArguments().getSInstructionArgument().isEmpty()) {

                Object first = si.getSInstructionArguments().getSInstructionArgument().get(0);

                if (first instanceof String) {
                    jumpTarget = (String) first;
                } else if (first instanceof SInstructionArgument) {
                    jumpTarget = ((SInstructionArgument) first).getValue();
                } else {
                    jumpTarget = String.valueOf(first);
                }
            }

            program.add(new BasicInstructions(op, var, jumpTarget));
            pc++;
        }
    }

    private static BasicInstructions.BasicOp mapOp(String xmlName) {
        switch (xmlName) {
            case "INCREASE":       return INC;
            case "DECREASE":       return DEC;
            case "NEUTRAL":        return NOOP;
            case "JUMP_NOT_ZERO":  return IF_NZ_GOTO;
            default:
                throw new IllegalArgumentException("Unsupported instruction name: " + xmlName);
        }
    }
}