package intentions.variableAssignment.evaluators;

import com.intellij.openapi.util.Pair;
import intentions.variableAssignment.GenericParameterPair;
import intentions.variableAssignment.OriginalFunctionResult;
import intentions.variableAssignment.ResolvedFunctionResult;
import utils.AppUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public abstract class TypeEvaluator {
    protected Optional<OriginalFunctionResult> optionalOriginalFunctionResult;
    protected ResolvedFunctionResult resolvedFunctionResult;

    public TypeEvaluator(Optional<OriginalFunctionResult> optionalOriginalFunctionResult,
                         ResolvedFunctionResult resolvedFunctionResult) {
        this.optionalOriginalFunctionResult = optionalOriginalFunctionResult;
        this.resolvedFunctionResult = resolvedFunctionResult;
    }

    protected List<GenericParameterPair> getGenericParameterPairs() {
        return optionalOriginalFunctionResult.map(functionResult ->
                AppUtils.zipInto(resolvedFunctionResult.getActualGenericTypeValues(),
                        functionResult.getActualGenericTypeValues(), GenericParameterPair::new))
                .orElse(Collections.emptyList());
    }

    public int getTotalCallExpressions() {
        return optionalOriginalFunctionResult
                .map(originalFunctionResult -> originalFunctionResult.getCallExpressions().size())
                .orElse(0);
    }

    /**
     * Replaces all generic types in the supplied value with its concrete type.
     */
    public static String replace(String value, List<GenericParameterPair> replacementPairs) {
        for (GenericParameterPair replacementPair : replacementPairs) {
            Pair<String, String> replacementInstruction = replacementPair.getReplacementInstruction();
            value = value.replaceAll(replacementInstruction.first, replacementInstruction.second);
        }
        return value;
    }

    public abstract String evaluate();


    /**
     * Returns a {@code TypeEvaluator} only if there is a function return type.
     */
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public static Optional<TypeEvaluator> get(Optional<OriginalFunctionResult> originalFunctionResult,
                                              Optional<ResolvedFunctionResult> resolvedFunctionResult) {
        if (resolvedFunctionResult.isPresent()) {
            if (originalFunctionResult.isPresent()) {
                // function called and has a function type.
                return Optional.of(new FunctionTypeEvaluator(originalFunctionResult, resolvedFunctionResult.get()));
            }
            // Not called but since it has a function return type, the full type is returned.
            return Optional.of(new FullTypeFunctionEvaluator(resolvedFunctionResult.get()));
        }
        return Optional.empty();
    }
}
