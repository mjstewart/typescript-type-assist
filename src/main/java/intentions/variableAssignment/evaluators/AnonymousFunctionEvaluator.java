package intentions.variableAssignment.evaluators;

import com.intellij.openapi.util.Pair;
import intentions.variableAssignment.OriginalFunctionResult;
import intentions.variableAssignment.ResolvedFunctionResult;
import utils.AppUtils;

import java.util.List;

/**
 * {@code AnonymousFunctionEvaluator} handles a function that is assigned to a variable such as a lambda function
 * or just a normal function.
 *
 * <pre>
 *     const me: (a: string) => (b: string) => (c: number) => boolean = specialFunction
 *
 * </pre>
 *
 *
 * which expects index 0 to refer to the first
 * anonymous function call which in this case would be to invoke (a: A) => ... which is the case when partially
 * applying a lambda.<
 */
public class AnonymousFunctionEvaluator extends ReturnTypeEvaluator {

    public AnonymousFunctionEvaluator(OriginalFunctionResult originalFunctionResult,
                                      ResolvedFunctionResult resolvedFunctionResult) {
        super(originalFunctionResult, resolvedFunctionResult);
    }

    private String getFinalReturnValue() {
        return resolvedFunctionResult.getLast().returnType();
    }

    @Override
    public String evaluate() {
        System.out.println("---- AnonymousFunctionEvaluator");

        // zip up each corresponding value which ties a call expression to what its return type would be.
        List<Pair<String, String>> pairs = AppUtils.zipInto(originalFunctionResult.getCallExpressionAsStrings(),
                resolvedFunctionResult.getFunctionValueReturnTypes(),
                Pair::create);

        // Guards against user code invoking function with too many call expressions.
        int lastInvokableIndex = pairs.size() - 1;

        System.out.println("pairs");
        pairs.forEach(System.out::println);

        String rawReturnValue = pairs.get(lastInvokableIndex).second;

//        return replace(rawReturnValue, getGenericParameterPairs());

        System.out.println("return value is: " + replace(rawReturnValue, getGenericParameterPairs()));
        return replace(rawReturnValue, getGenericParameterPairs());
    }
}
