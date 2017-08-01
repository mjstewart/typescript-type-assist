package intentions.variableAssignment.evaluators;

import com.intellij.openapi.util.Pair;
import intentions.variableAssignment.GenericParameterPair;
import intentions.variableAssignment.OriginalFunctionResult;
import intentions.variableAssignment.ResolvedFunctionResult;
import utils.AppUtils;

import java.util.List;

public abstract class ReturnTypeEvaluator {
    protected OriginalFunctionResult originalFunctionResult;
    protected ResolvedFunctionResult resolvedFunctionResult;

    public ReturnTypeEvaluator(OriginalFunctionResult originalFunctionResult,
                               ResolvedFunctionResult resolvedFunctionResult) {
        this.originalFunctionResult = originalFunctionResult;
        this.resolvedFunctionResult = resolvedFunctionResult;
    }

    protected List<GenericParameterPair> getGenericParameterPairs() {
        return AppUtils.zipInto(resolvedFunctionResult.getGenericTypes(), originalFunctionResult.getActualGenericTypeValues(),
                GenericParameterPair::new);
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
}
