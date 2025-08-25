package s.emulator.api;

import s.emulator.api.dto.Dtos;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface Engine {

    void loadProgram(File xml) throws Exception;
    String currentProgramName();
    boolean hasProgram();

    Dtos.ProgramSummary getProgramSummary();
    int getMaxDegree();
    List<String> getInputsUsed(int degree);

    Dtos.ExpansionPreview previewExpansion(int degree);

    Dtos.RunResult run(int degree, Map<String,Integer> inputsByName);

    List<Dtos.RunHistoryEntry> getHistory();
    void clearHistory();
}
