package intentions.variableAssignment;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import com.intellij.openapi.util.Pair;

import java.util.List;

public class StandardFunction {
    private TypeScriptFunction function;

    // <T, U>
    private List<String> genericTypes;

    // <string, number>
    private List<String> typeParameterValues;

    // Given [T -> string, U, number], a pair is [\bT\b, string] which means find T and replace with string.
    private List<Pair<String, String>> genericTypeReplacementPairs;


    public StandardFunction(TypeScriptFunction function) {
        this.function = function;

    }
}
