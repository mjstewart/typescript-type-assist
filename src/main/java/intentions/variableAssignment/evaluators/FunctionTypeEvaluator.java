package intentions.variableAssignment.evaluators;

import intentions.variableAssignment.OriginalFunctionResult;
import intentions.variableAssignment.ResolvedFunctionResult;

import java.util.List;
import java.util.Optional;

/**
 * To be used to evaluate both normal functions with a parameter list and lambda style functions.
 *
 * <pre>
 *     {@code function special<A, B, C, D>(a: A, b: B): (a: A) => (b: B) => (c: C) => D}
 *     {@code const addMe = (a: number) => (b: number): number => a + b}
 * </pre>
 *
 * <p>The evaluation strategy is as follows</p>
 *
 * <ul>
 *     <li>0 call expressions -> return type is index 0 in getAllReturnTypes list.</li>
 *     <li>1 call expressions -> return type is index 1 in getAllReturnTypes list.</li>
 *     ...
 *     <li>n call expressions -> return type is index n in getAllReturnTypes list.</li>
 * </ul>
 *
 * <p>
 *     This works because all {@code FunctionValue}s know how to generate a list of all possible return types. From
 *     here its just a matter of counting the number of call expressions which determines the final return type.
 *     An important implementation detail is that its the <b>size</b> of the call expression list which determines the
 *     index to get in the corresponding return value list.
 * </p>
 */
public class FunctionTypeEvaluator extends TypeEvaluator {

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    public FunctionTypeEvaluator(Optional<OriginalFunctionResult> originalFunctionResult,
                                 ResolvedFunctionResult resolvedFunctionResult) {
        super(originalFunctionResult, resolvedFunctionResult);
    }

    @Override
    public String evaluate() {
        System.out.println("---- FunctionTypeEvaluator");

        List<String> allReturnTypes = resolvedFunctionResult.getFunctionValue().getAllReturnTypes();

        // Guards against user code invoking function with too many call expressions.
        int index = Math.min(getTotalCallExpressions(), allReturnTypes.size() - 1);

        String rawReturnValue = allReturnTypes.get(index);
        return replace(rawReturnValue, getGenericParameterPairs());
    }
}
