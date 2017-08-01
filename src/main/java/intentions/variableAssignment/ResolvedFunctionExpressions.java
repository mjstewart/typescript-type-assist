package intentions.variableAssignment;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import intentions.variableAssignment.functions.FunctionExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResolvedFunctionExpressions extends ResolvedFunctionTypes {

    public ResolvedFunctionExpressions(@NotNull Collection<TypeScriptFunction> typeScriptFunctions) {
        functionValues.addAll(typeScriptFunctions.stream()
                .map(FunctionExpression::new)
                .collect(Collectors.toList()));
    }

    /**
     * This method handles case where a function is assigned to a variable.
     *
     * <pre>
     *     const fn: (a: string) => (b: string) => (c: number) => boolean = ...
     *
     *     // functionTypes
     *     index 0 - (a: string) => (b: string) => (c: number) => boolean
     *     index 1 - (b: string) => (c: number) => boolean
     *     index 2 - (c: number) => boolean
     *
     *     // Consider the below call expressions
     *     - The type of fn with no call expression is index 0.
     *     - 1 call expression fn("first") has the type of index 1
     *     - fully applied with 2 calls fn("first")("second") means return value is index 2's return value of boolean.
     * </pre>
     */
    @Override
    public String getReturnType(List<CallExpressionWithArgs> callExpressionWithArgs) {
        System.out.println("#### ResolvedFunctionExpressions getReturnTypeForFunctionVariable: callExpressionWithArgs = " + callExpressionWithArgs);
        System.out.println("#### ResolvedFunctionExpressions getReturnTypeForFunctionVariable: functionTypes = " + functionValues);
        System.out.println("#### ResolvedFunctionExpressions getReturnFunctionType COMPARISONS: ");


        if (callExpressionWithArgs.isEmpty()) {
            return functionValues.get(0).signature();
        }
        if (callExpressionWithArgs.size() == functionValues.size()) {
            // TODO document why i am doing this. an expresson is like a lambda assigned to a variable like
            // const addMe = (a: number) => (b: number): number => a + b;, but when reassigned to anothr variable it should be
            // addMe2 = (a: number) => (b: number) => number = addMe;
            // function is fully applied. get the value after the last =>
            String[] split = getLast().signature().split("=>");
            String lastReturnValue = split[split.length - 1].trim();
            return lastReturnValue;
        }
        return format(functionValues.get(callExpressionWithArgs.size()).signature());
    }

    private String format(String value) {
        return value.substring(0, value.lastIndexOf("=>")).trim();
    }

    @Override
    public String toString() {
        return functionValues.toString();
    }
}
