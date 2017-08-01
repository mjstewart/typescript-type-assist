package intentions.variableAssignment.evaluators;

import com.intellij.openapi.util.Pair;
import intentions.variableAssignment.OriginalFunctionResult;
import intentions.variableAssignment.ResolvedFunctionResult;
import utils.AppUtils;

import java.util.List;

/**
 * To be used to evaluate standard functions with a parameter list such as
 *
 * <pre>
 *     {@code function special<A, B, C, D>(a: A, b: B): (a: A) => (b: B) => (c: C) => D}
 * </pre>
 *
 * <p>Index 0 in the call expression list refers to the functions arguments {@code (a: A, b: B)} with the corresponding
 * return type being available in index 0 in the function value list.</p>
 *
 * <p>This is in contrast to the {@code AnonymousFunctionEvaluator} which expects index 0 to refer to the first
 * anonymous function call which in this case would be to invoke (a: A) => ... which is the case when partially
 * applying a lambda.</p>
 */
public class StandardFunctionEvaluator extends ReturnTypeEvaluator {

    public StandardFunctionEvaluator(OriginalFunctionResult originalFunctionResult,
                                     ResolvedFunctionResult resolvedFunctionResult) {
        super(originalFunctionResult, resolvedFunctionResult);
    }

    private String getFinalReturnValue() {
        return resolvedFunctionResult.getLast().returnType();
    }

    @Override
    public String evaluate() {
        System.out.println("---- StandardFunctionEvaluator");
        // zip up each corresponding value which ties a call expression to what its return type would be.
        List<Pair<String, String>> pairs = AppUtils.zipInto(originalFunctionResult.getCallExpressionAsStrings(),
                resolvedFunctionResult.getFunctionValueSignatures(),
                Pair::create);

        // Simplifies logic as index of last call expression determines the resolution result in Pair.2.
        pairs.add(Pair.create("final return value", getFinalReturnValue()));

        // Guards against user code invoking function with too many call expressions, -1 to bring back to array index.
        int lastInvokableIndex = Math.min(pairs.size() - 1, originalFunctionResult.indexOfLastCallExpression());

        System.out.println("pairs");
        pairs.forEach(System.out::println);

        String rawReturnValue = pairs.get(lastInvokableIndex).second;

        return replace(rawReturnValue, getGenericParameterPairs());
    }
}
