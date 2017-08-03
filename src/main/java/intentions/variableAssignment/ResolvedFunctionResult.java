package intentions.variableAssignment;

import intentions.variableAssignment.functions.FunctionValue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class ResolvedFunctionResult {
    private List<String> actualGenericTypeValues;
    private FunctionValue functionValue;

    private ResolvedFunctionResult(List<String> actualGenericTypeValues, FunctionValue functionValue) {
        this.actualGenericTypeValues = actualGenericTypeValues;
        this.functionValue = functionValue;
    }

    public static Optional<ResolvedFunctionResult> of(List<String> genericTypes, Optional<FunctionValue> functionValue) {
        return functionValue.map(value -> new ResolvedFunctionResult(genericTypes, value));
    }

    public static Optional<ResolvedFunctionResult> of(Optional<FunctionValue> functionValue) {
        return functionValue.map(value -> new ResolvedFunctionResult(Collections.emptyList(), value));
    }

    public List<String> getActualGenericTypeValues() {
        return actualGenericTypeValues;
    }

    public FunctionValue getFunctionValue() {
        return functionValue;
    }

    @Override
    public String toString() {
        return "ResolvedFunctionResult{" +
                "actualGenericTypeValues=" + actualGenericTypeValues +
                ", functionValue.getAllReturnTypes()=" + functionValue.getAllReturnTypes() +
                '}';
    }
}
