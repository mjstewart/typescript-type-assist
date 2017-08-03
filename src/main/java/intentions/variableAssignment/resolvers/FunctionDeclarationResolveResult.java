package intentions.variableAssignment.resolvers;

import intentions.variableAssignment.OriginalFunctionResult;
import intentions.variableAssignment.ResolvedFunctionResult;
import intentions.variableAssignment.evaluators.TypeEvaluator;

import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
public class FunctionDeclarationResolveResult {
    private Optional<OriginalFunctionResult> originalFunctionResult;
    private Optional<ResolvedFunctionResult> resolvedFunctionResult;
    private Optional<TypeEvaluator> typeEvaluator;

    private FunctionDeclarationResolveResult(Optional<OriginalFunctionResult> originalFunctionResult,
                                             Optional<ResolvedFunctionResult> resolvedFunctionResult,
                                             Optional<TypeEvaluator> typeEvaluator) {
        this.originalFunctionResult = originalFunctionResult;
        this.resolvedFunctionResult = resolvedFunctionResult;
        this.typeEvaluator = typeEvaluator;
    }

    public static FunctionDeclarationResolveResult of(Optional<OriginalFunctionResult> originalFunctionResult,
                                                      Optional<ResolvedFunctionResult> resolvedFunctionResult,
                                                      Optional<TypeEvaluator> typeEvaluator) {
        return new FunctionDeclarationResolveResult(originalFunctionResult, resolvedFunctionResult, typeEvaluator);
    }

    public static FunctionDeclarationResolveResult empty() {
        return of(Optional.empty(), Optional.empty(), Optional.empty());
    }

    public Optional<OriginalFunctionResult> getOriginalFunctionResult() {
        return originalFunctionResult;
    }

    public Optional<ResolvedFunctionResult> getResolvedFunctionResult() {
        return resolvedFunctionResult;
    }

    public Optional<TypeEvaluator> getTypeEvaluator() {
        return typeEvaluator;
    }
}
