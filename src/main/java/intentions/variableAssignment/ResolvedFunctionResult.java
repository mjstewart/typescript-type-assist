package intentions.variableAssignment;

import intentions.variableAssignment.functions.FunctionValue;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ResolvedFunctionResult {
    private List<String> genericTypes;
    private List<FunctionValue> functionValues;

    private ResolvedFunctionResult(List<String> genericTypes, List<FunctionValue> functionValues) {
        this.genericTypes = genericTypes;
        this.functionValues = functionValues;
    }

    public static Optional<ResolvedFunctionResult> of(List<String> genericTypes, List<FunctionValue> functionValues) {
        return functionValues.isEmpty() ? Optional.empty() :
                Optional.of(new ResolvedFunctionResult(genericTypes, functionValues));
    }

    public static Optional<ResolvedFunctionResult> of(List<FunctionValue> functionValues) {
        return functionValues.isEmpty() ? Optional.empty() :
                Optional.of(new ResolvedFunctionResult(Collections.emptyList(), functionValues));
    }

    public FunctionValue getLast() {
        return functionValues.get(functionValues.size() - 1);
    }

    public List<String> getGenericTypes() {
        return genericTypes;
    }

    public List<FunctionValue> getFunctionValues() {
        return functionValues;
    }

    public List<String> getFunctionValueSignatures() {
        return functionValues.stream().map(FunctionValue::signature).collect(Collectors.toList());
    }

    public List<String> getFunctionValueReturnTypes() {
        return functionValues.stream().map(FunctionValue::returnType).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "ResolvedFunctionResult{" +
                "genericTypes=" + genericTypes +
                ", functionValues=" + functionValues +
                '}';
    }
}
