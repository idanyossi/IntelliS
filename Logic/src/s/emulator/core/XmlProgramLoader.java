package s.emulator.core;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import s.emulator.jaxb.SInstruction;
import s.emulator.jaxb.SInstructions;
import s.emulator.jaxb.SProgram;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static s.emulator.core.BasicInstructions.BasicOp.*;

public class XmlProgramLoader {
    public final List<Instruction> program = new ArrayList<>();
    public final Map<String,Integer> labels = new HashMap<>();

    public XmlProgramLoader(List<Instruction> program, Map<String,Integer> labels) {
        this.program.addAll(program);
        this.labels.putAll(labels);
    }

    public void load(File xmlFile) throws Exception{
        JAXBContext context = JAXBContext.newInstance(SProgram.class);
        Unmarshaller unmarshaller = context.createUnmarshaller();

        SProgram sProgram = (SProgram)unmarshaller.unmarshal(xmlFile);

        labels.clear();
        program.clear();

        SInstructions sin = sProgram.getSInstructions();
        if(sin != null){return;}

        int pc = 0;
        for (SInstruction si : sin.getSInstruction()){
            String xmlOper = si.getName();
            BasicInstructions.BasicOp op = mapOp(xmlOper);

            String var = si.getSVariable();
            String jump = si.getSLabel();

            if(jump != null && !jump.isEmpty() && op != IF_NZ_GOTO){
                labels.put(jump,pc);
            }

            program.add(new BasicInstructions(op,var,jump));
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
