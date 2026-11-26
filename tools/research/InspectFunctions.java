import ghidra.app.script.GhidraScript;
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.symbol.Reference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Export selected functions and their callers/callees for local executable research. */
public class InspectFunctions extends GhidraScript {
    public void run() throws Exception {
        String[] args = getScriptArgs();
        Path output = Paths.get(args[0]);
        Files.createDirectories(output);
        DecompInterface decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);
        try {
            StringBuilder index = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                boolean defineFunction = args[i].startsWith("function:");
                Address address = toAddr(defineFunction ? args[i].substring(9) : args[i]);
                Function function = getFunctionContaining(address);
                if (function == null && defineFunction) {
                    disassemble(address);
                    function = createFunction(address, null);
                }
                index.append("\nAddress ").append(address).append("\n");
                for (Reference reference : getReferencesTo(address)) {
                    index.append("xref ").append(reference.getFromAddress()).append(" ")
                        .append(getFunctionContaining(reference.getFromAddress())).append("\n");
                }
                if (function == null) continue;
                index.append("Function ").append(function.getEntryPoint()).append("\n");
                for (Function called : function.getCalledFunctions(monitor)) {
                    index.append("calls ").append(called.getEntryPoint()).append(" ")
                        .append(called.getName()).append("\n");
                }
                DecompileResults result = decompiler.decompileFunction(function, 60, monitor);
                if (result.decompileCompleted()) {
                    Files.writeString(output.resolve(function.getEntryPoint() + ".c"),
                        result.getDecompiledFunction().getC());
                } else {
                    index.append("Failed: ").append(result.getErrorMessage()).append("\n");
                }
            }
            Files.writeString(output.resolve("index.txt"), index.toString());
            println(index.toString());
        } finally {
            decompiler.dispose();
        }
    }
}
