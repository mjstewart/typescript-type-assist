package intentions.variableAssignment.evaluators;

import intentions.variableAssignment.ResolvedFunctionResult;

/**
 * Out of all possible return types, return the first return type which represents an uncalled function or a function
 * that has only been called with its initial arguments depending on the context.
 */
public class FullTypeFunctionEvaluator extends TypeEvaluator {

    public FullTypeFunctionEvaluator(ResolvedFunctionResult resolvedFunctionResult) {
        super(null, resolvedFunctionResult);
    }

    @Override
    public String evaluate() {
        System.out.println("--- FullTypeFunctionEvaluator");
        return resolvedFunctionResult.getFunctionValue().getAllReturnTypes().get(0);
    }
}
