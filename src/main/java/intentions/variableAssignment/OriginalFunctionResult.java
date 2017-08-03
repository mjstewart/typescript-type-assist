package intentions.variableAssignment;

import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptTypeArgumentList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contains info about a function call before it is resolved.
 *
 * <pre>
 *    resolved function:
 *   {@code function print<T, A>(): (t: T, a: A) => (c: A) => (d: T) => string}
 *
 *    actualGenericTypeValues: [string, number] T is string, A is number. This will allow the generic types to be
 *    replaced with these concrete values.
 *
 *    callExpressionsForVariable: Since the function may be partially applied, this list will contain all partial
 *    application arguments.
 * </pre>
 */
public class OriginalFunctionResult {
    private List<CallExpressionWithArgs> callExpressions;
    private List<String> actualGenericTypeValues;

    private OriginalFunctionResult(List<CallExpressionWithArgs> callExpressions, List<String> actualGenericTypeValues) {
        this.callExpressions = callExpressions;
        this.actualGenericTypeValues = actualGenericTypeValues;
    }

    public static Optional<OriginalFunctionResult> of(List<CallExpressionWithArgs> callExpressions) {
        if (callExpressions.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new OriginalFunctionResult(callExpressions, getActualGenericTypeValues(callExpressions)));
    }

    public List<CallExpressionWithArgs> getCallExpressions() {
        return callExpressions;
    }

    private static JSCallExpression getRootCallExpression(List<CallExpressionWithArgs> callExpressions) {
        return callExpressions.get(0).getJsCallExpression();
    }

    public List<String> getActualGenericTypeValues() {
        return actualGenericTypeValues;
    }

    /**
     * In the list of call expressions which are function calls such as {@code doStuff<string, number, Cat>},
     * find the call expression that has the concrete generic type argument values. It's always expected that
     * out of many call expressions, there will be only 1 containing the concrete type values to handle cases where
     * a series of calls to variables assigned to functions.
     *
     *
     * <pre>
     *     The first call expression should be provided, as that is the one that contains the TypescriptTypeArgumentList.
     *
     *     // This contains 3 call expressions, any one can be used as they all contain the generic type argument list,
     *     // However you should try use the first as its the simplest.
     *     1. print4<string, number>()
     *     2. print4<string, number>()("4", 5)
     *     3. print4<string, number>()("4", 5)("sss")
     * </pre>
     *
     * @return List of the type parameters such as List(string, number, Cat).
     */
    private static List<String> getActualGenericTypeValues(List<CallExpressionWithArgs> callExpressions) {
        return callExpressions.stream()
                .map(call -> Optional.ofNullable(PsiTreeUtil.findChildOfType(getRootCallExpression(callExpressions), TypeScriptTypeArgumentList.class)))
                .filter(Optional::isPresent)
                .limit(1)
                .map(Optional::get)
                .map(TypeScriptTypeArgumentList::getTypeArguments)
                .flatMap(Arrays::stream)
                .map(PsiElement::getText)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "OriginalFunctionResult{" +
                "callExpressions=" + callExpressions +
                ", getActualGenericTypeValues=" + getActualGenericTypeValues() +
                '}';
    }
}
