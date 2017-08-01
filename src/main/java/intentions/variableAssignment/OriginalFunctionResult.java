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

    private OriginalFunctionResult(List<CallExpressionWithArgs> callExpressions) {
        this.callExpressions = callExpressions;
    }

    public static Optional<OriginalFunctionResult> of(List<CallExpressionWithArgs> callExpressions) {
        return callExpressions.isEmpty() ? Optional.empty() : Optional.of(new OriginalFunctionResult(callExpressions));
    }

    public List<CallExpressionWithArgs> getCallExpressions() {
        return callExpressions;
    }

    public int indexOfLastCallExpression() {
        return callExpressions.size() - 1;
    }

    public List<String> getCallExpressionAsStrings() {
        return callExpressions.stream().map(CallExpressionWithArgs::getArguments).collect(Collectors.toList());
    }

    public JSCallExpression getRootCallExpression() {
        return callExpressions.get(0).getJsCallExpression();
    }

    /**
     * {@code doStuff<string, number, Cat>} ....
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
    public List<String> getActualGenericTypeValues() {
        return PsiTreeUtil.findChildrenOfType(getRootCallExpression(), TypeScriptTypeArgumentList.class).stream()
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
