package intentions.variableAssignment;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import intentions.variableAssignment.functions.FunctionValue;
import intentions.variableAssignment.functions.StandardFunction;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResolvedStandardFunctions extends ResolvedFunctionTypes {

    /**
     * @param function            If supplied, is added as the value in index 0 in the {@code functionTypes} list.
     * @param typeScriptFunctions Any more function types which should be added to the {@code functionTypes} list.
     */
    public ResolvedStandardFunctions(TypeScriptFunction function,
                                  @NotNull Collection<TypeScriptFunction> typeScriptFunctions) {
        if (function != null) {
            functionValues.add(new StandardFunction(function));
        }
        functionValues.addAll(typeScriptFunctions.stream()
                .map(StandardFunction::new)
                .collect(Collectors.toList()));
    }

    public ResolvedStandardFunctions(@NotNull Collection<TypeScriptFunction> typeScriptFunctions) {
        this(null, typeScriptFunctions);
    }

    @Override
    public FunctionValue getLast() {
        return functionValues.get(functionValues.size() - 1);
    }

    /**
     * This example explains how the correct return type is resolved.
     * <p>
     * <pre>
     *  {@code function special<A, B, C, D>(a: A, b: B): (a: A) => (b: B) => (c: C) => D {
     *         ....
     *    }}
     *
     *   // The functionTypes are taken from the return type in this example.
     *   functionTypes = [(a: A) => (b: B) => (c: C) => D, (b: B) => (c: C) => D, (c: C) => D]
     *   index 0 - (a: A) => (b: B) => (c: C) => D
     *   index 1 - (b: B) => (c: C) => D
     *   index 2 - (c: C) => D
     *
     *  {@code special<string, string, number, boolean>("a", "b");}
     *   callExpressionWithArgs = [("a", "b")]
     *
     * </pre>
     * <p>
     * <p>Index 0 in callExpressionWithArgs refers to the arguments supplied to the first function call, therefore
     * index 0 in functionTypes is the return value.</p>
     * <p>
     * <p>If the returned function was called again, 2 elements would be in callExpressionWithArgs and index 1 in
     * functionTypes is the return value.</p>
     * <p>
     * <p>The special case is if the function is fully applied which means the last function with argument c
     * is invoked. This the function is fully applied, index 2 contains the final return type of D.</p>
     */
    @Override
    public String getReturnType(List<CallExpressionWithArgs> callExpressionWithArgs) {
        System.out.println("#### ResolvedStandardFunctions  getReturnFunctionType: callExpressionWithArgs = " + callExpressionWithArgs);
        System.out.println("#### ResolvedStandardFunctions getReturnFunctionType: functionTypes = " + functionValues);

        if (callExpressionWithArgs.size() == functionValues.size() + 1) {
            // function is fully applied.
            System.out.println("#### ResolvedStandardFunctions callExpressions and functionValues same size, getting last");
            System.out.println("#### ResolvedStandardFunctions LAST = " + getLast());
            return getLast().returnType();
        }
        return functionValues.get(callExpressionWithArgs.size() - 1).signature();
    }

    @Override
    public String toString() {
        return functionValues.toString();
    }
}
