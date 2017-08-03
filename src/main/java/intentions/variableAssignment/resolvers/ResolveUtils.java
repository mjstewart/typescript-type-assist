package intentions.variableAssignment.resolvers;

import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSExpressionStatement;
import com.intellij.lang.javascript.psi.JSType;
import com.intellij.lang.javascript.psi.ecma6.*;
import com.intellij.lang.javascript.psi.ecma6.impl.TypeScriptParameterListImpl;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import intentions.variableAssignment.CallExpressionWithArgs;
import intentions.variableAssignment.OptionalPipeline;
import intentions.variableAssignment.functions.FunctionExpression;
import intentions.variableAssignment.functions.FunctionType;
import intentions.variableAssignment.functions.FunctionValue;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ResolveUtils {
    /**
     * {@code doStuff<T, U, V>} ....
     *
     * @return List of the type parameters such as List(T, U, V).
     */
    public static List<String> getGenericTypeParameters(@NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptTypeParameterList.class))
                .map(TypeScriptTypeParameterList::getTypeParameters)
                .map(Arrays::stream)
                .map(s -> s.map(TypeScriptTypeParameter::getName))
                .map(params -> params.collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }


    public static JSExpressionStatement getExpression(@NotNull PsiElement psiElement) {
        JSExpressionStatement parentExpression = PsiTreeUtil.getTopmostParentOfType(psiElement, JSExpressionStatement.class);
        if (parentExpression != null) return parentExpression;
        return (psiElement.getPrevSibling() instanceof JSExpressionStatement) ? (JSExpressionStatement) psiElement.getPrevSibling() : null;
    }

    /**
     * Collects all {@code JSCallExpression}s and returns them in the reverse order to appear in the same order
     * as they are applied to the function invocations.
     *
     * <p>If a function returns a series of partially applied functions it will look like this without reversing.</p>
     * <pre>
     *     {@code function doThis<A, B>(a: A, x: B): (b: B) => (c: A) => B}
     *
     *     {@code doThis<string, number>("#", "3")("4")}
     *
     *      callExpressions = [("4"), ("#", "3")] - in the opposite order as they are applied, so reverse fixes this.
     * </pre>
     */
    public static List<CallExpressionWithArgs> getCallExpressions(@NotNull PsiElement expression) {
        Function<JSCallExpression, Optional<CallExpressionWithArgs>> mapper = call ->
                call.getArgumentList() == null ? Optional.empty() : Optional.of(new CallExpressionWithArgs(call));

        return PsiTreeUtil.findChildrenOfType(expression, JSCallExpression.class).stream()
                .map(mapper)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.collectingAndThen(Collectors.toList(), list -> {
                    Collections.reverse(list);
                    return list;
                }));
    }

    /**
     * Calls {@link #getFullFunctionValue(PsiElement)} first, if present returns it otherwise goes on to return the result of
     * {@link #getFunctionExpression(PsiElement)}
     */
    public static Optional<FunctionValue> getFunctionValue(@NotNull PsiElement element) {
        return OptionalPipeline.of(getFullFunctionValue(element))
                .orElse(getFunctionExpression(element))
                .get();
    }

    /**
     * If the supplied {@code PsiElement} is a {@code TypeScriptFunctionType}, the full function return type is
     * wrapped within a {@code FunctionValue}. Additionally, if the function type has a parameter list, the
     * parameters are prepended to the return type to provide the full signature of the uncalled function.
     */
    public static Optional<FunctionValue> getFullFunctionValue(@NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptParameterListImpl.class))
                .flatMap(params -> Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptFunctionType.class))
                        .map(functionType -> params.getText() + " => " + functionType.getText()))
                .map(signature -> new FunctionType(signature, getGenericTypeParameters(element)));
    }

    /**
     * If the supplied {@code PsiElement} is a {@code TypeScriptFunctionExpression}, the full expression is
     * wrapped within a {@code FunctionValue}.
     */
    public static Optional<FunctionValue> getFunctionExpression(@NotNull PsiElement element) {
        return Arrays.stream(element.getChildren())
                .filter(e -> e instanceof TypeScriptFunctionExpression)
                .map(PsiElement::getText)
                .map(signature -> (FunctionValue) new FunctionExpression(signature, getGenericTypeParameters(element)))
                .findFirst();

//        System.out.println("FIRST value found");
//        System.out.println(first);
//
//        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptFunctionExpression.class))
//                .map(TypeScriptFunctionExpression::getText)
//                .map(signature -> new FunctionExpression(signature, getGenericTypeParameters(element)));



    }

    /**
     * Gets the return type of the supplied function.
     */
    public static String getFunctionReturnType(@NotNull TypeScriptFunction function) {
        return function.getReturnType() == null ? "any" : function.getReturnType().getTypeText(JSType.TypeTextFormat.CODE);
    }

}
