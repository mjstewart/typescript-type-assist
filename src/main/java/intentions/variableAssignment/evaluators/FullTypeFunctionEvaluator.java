package intentions.variableAssignment.evaluators;

import intentions.variableAssignment.ResolvedFunctionResult;

public class FullTypeFunctionEvaluator extends ReturnTypeEvaluator {

    public FullTypeFunctionEvaluator(ResolvedFunctionResult resolvedFunctionResult) {
        super(null, resolvedFunctionResult);
    }

    @Override
    public String evaluate() {
        System.out.println("--- FullTypeFunctionEvaluator");
        System.out.println(resolvedFunctionResult.getFunctionValues().get(0).signature());

        return resolvedFunctionResult.getFunctionValues().get(0).signature();
    }
}
