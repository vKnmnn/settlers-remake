import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.symbol.Reference;
import java.nio.file.*;
import java.util.*;

public class InspectGame extends GhidraScript {
    public void run() throws Exception {
        Path out = Paths.get(getScriptArgs()[0]);
        Files.createDirectories(out);
        DecompInterface decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);
        Set<Function> functions = new LinkedHashSet<>();
        StringBuilder index = new StringBuilder("Program: " + currentProgram.getName() + "\nFunctions: " + currentProgram.getFunctionManager().getFunctionCount() + "\n");
        for (int i = 1; i < getScriptArgs().length; i++) {
            Address address = toAddr(getScriptArgs()[i]);
            index.append("\nAnchor ").append(address).append("\n");
            for (Reference reference : getReferencesTo(address)) {
                Function function = getFunctionContaining(reference.getFromAddress());
                index.append(reference.getFromAddress()).append(" -> ").append(function).append("\n");
                if (function != null) functions.add(function);
            }
        }
        for (Function function : functions) {
            DecompileResults result = decompiler.decompileFunction(function, 45, monitor);
            if (result.decompileCompleted()) {
                Files.writeString(out.resolve(function.getEntryPoint() + ".c"), result.getDecompiledFunction().getC());
            } else index.append("Failed: ").append(function).append(' ').append(result.getErrorMessage()).append("\n");
        }
        Files.writeString(out.resolve("index.txt"), index.toString());
        decompiler.dispose();
        println(index.toString());
    }
}
