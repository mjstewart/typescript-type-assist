package intentions.variableAssignment.resolvers;

import com.intellij.lang.javascript.psi.JSCallExpression;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptVariable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.util.PsiTreeUtil;
import intentions.variableAssignment.CallExpressionWithArgs;
import intentions.variableAssignment.OptionalPipeline;
import intentions.variableAssignment.OriginalFunctionResult;
import intentions.variableAssignment.ResolvedFunctionResult;
import intentions.variableAssignment.evaluators.TypeEvaluator;
import intentions.variableAssignment.functions.FunctionValue;
import intentions.variableAssignment.functions.NonFunctionReturnType;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Handles finding the originally declared function and creating the appropriate {@code TypeEvaluator}.
 */
public class FunctionDeclarationResolver {

    private static class CallCollector {
        /*
         * Contains all function calls that occur in a chain of partially applied assignments going up to the main
         * function definition.
         *
         * function addStuff....
         *
         * const a = addStuff(5)
         * const b = a(6)
         * const c = b(7)
         *
         * callExpressions = [(5), (6), (7)] - Order is from the main function going down to the last call.
         */
        private LinkedList<CallExpressionWithArgs> callExpressions = new LinkedList<>();
        private FunctionValue functionValue;

        private void addFirstCalls(List<CallExpressionWithArgs> callExpressions) {
            callExpressions.forEach(call -> this.callExpressions.addFirst(call));
        }

        private void addLastCalls(List<CallExpressionWithArgs> callExpressions) {
            callExpressions.forEach(call -> this.callExpressions.addLast(call));
        }
    }

    /**
     * Starting from a leaf call in step 1, walk back up through a series of function calls until the original root
     * function declaration is found which contains all the type information. The whole point is to
     * reconstruct a series of function calls back into its original form of calling the function all in 1 statement.
     *
     * <p>Consider the following example which is read bottom to top from steps 1 to 5.</p>
     *
     * <pre>
     *
     * 5. function special<A, B, C, D>(a: A, b: B): (a: A) => (b: B) => (c: C) => D {
     *    return ;
     *   }
     *
     * 4. const a1: (a: string) => (b: string) => (c: number) => number = special<string, string, number, number>("hi", "there");
     * 3. const a2: (b: string) => (c: number) => number = a1("test");
     * 2. const a3: (c: number) => number = a2("me");
     *
     * 1. a3(500);  -- Initial trigger, this method is then passed in the a3 variable which begins the recursion upwards
     *    to step 5 which is the original function declaration.
     * </pre>
     *
     * <p>call expressions are collected and kept in order hence a linked list is used. The {@code CallCollector.callExpressions}
     * contains {@code ("hi", "there"), ("test"), ("me")} which is the same order of calls if they were done inline.
     * The caller must add {@code a3(500)} to the end of this list to create the same result as calling all functions inline as below.</p>
     *
     * <pre>
     *     special<string, string, number, number>("hi", "there")("test")("me")(500)
     *
     *     is the same as calling the chain 0 to 4 as above.
     * </pre>
     *
     * <p>Since the exact sequence of call expressions is collected, its simply a matter of getting the corresponding
     * return value of the return type list which one of the {@code TypeEvaluator}s can do.</p>
     *
     * @param element This element to begin walking back up the function call chain.
     * @return The {@code CallCollector} if the declared function was found.
     */
    public static Optional<CallCollector> findFunctionDeclaration(@NotNull PsiElement element) {
        return findFunctionDeclaration(element, new CallCollector());
    }


    private static Optional<CallCollector> findFunctionDeclaration(@NotNull PsiElement element, CallCollector collector) {
        System.out.println("%%%%%%%%%%%%%%%%%%%%%% findFunctionDeclaration for: " + element.getText() + " , type: " + element.getClass());

        // Function expressions will be nested deeper so check this first as its more specific.
        Optional<FunctionValue> functionExpression = ResolveUtils.getFunctionExpression(element);
        System.out.println("functionExpression: " + functionExpression);
        if (functionExpression.isPresent()) {
            System.out.println("functionExpression FOUND returning a result!");
            collector.functionValue = functionExpression.get();
            return Optional.of(collector);
        }

        if (element instanceof TypeScriptFunction) {
            Optional<FunctionValue> fullFunction = ResolveUtils.getFullFunctionValue(element);
            System.out.println("fullFunction: " + fullFunction);
            if (fullFunction.isPresent()) {
                System.out.println("fullFunction FOUND returning a result!");
                collector.functionValue = fullFunction.get();
            } else {
                System.out.println("NonFunctionReturnType FOUND returning a result!");
                TypeScriptFunction function = (TypeScriptFunction) element;
                collector.functionValue = new NonFunctionReturnType(ResolveUtils.getFunctionReturnType(function),
                        ResolveUtils.getGenericTypeParameters(function));
            }

            return Optional.of(collector);
        }

        if (element instanceof TypeScriptVariable) {
            System.out.println("element is a TypeScriptVariable");
            // Keep trying to resolve assignment values further
            collector.addFirstCalls(ResolveUtils.getCallExpressions(element));

            Optional<PsiElement> resolution = OptionalPipeline.of(tryResolveCallExpression(element))
                    .orElse(tryResolveReferenceExpression(element))
                    .get();

            System.out.println("Calls: " + collector.callExpressions);
            System.out.println("resolution result: " + resolution);
            if (resolution.isPresent()) {
                System.out.println("calling recursively again");
                return findFunctionDeclaration(resolution.get(), collector);
            }
        }
        return Optional.empty();
    }

    private static Optional<PsiElement> tryResolveCallExpression(@NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, JSCallExpression.class))
                .flatMap(call -> Optional.ofNullable(PsiTreeUtil.findChildOfType(call, JSReferenceExpression.class)))
                .map(PsiReference::resolve);
    }

    private static Optional<PsiElement> tryResolveReferenceExpression(@NotNull PsiElement element) {
        // Find the last reference which is an assignment statement that could lead to another function.
        return PsiTreeUtil.findChildrenOfType(element, JSReferenceExpression.class).stream()
                .reduce((a, b) -> b)
                .map(PsiReference::resolve);
    }


    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static FunctionDeclarationResolveResult getTypeEvaluator(@NotNull PsiElement originalElement, Optional<CallCollector> collector) {
        if (!collector.isPresent()) {
            return FunctionDeclarationResolveResult.empty();
        }
        // Add the original function calls on to the end since it represents the final call if all calls were on the same line.
        CallCollector callCollector = collector.get();
        callCollector.addLastCalls(ResolveUtils.getCallExpressions(originalElement));

        Optional<OriginalFunctionResult> originalFunctionResult = OriginalFunctionResult.of(callCollector.callExpressions);
        Optional<ResolvedFunctionResult> resolvedFunctionResult = ResolvedFunctionResult.of(callCollector.functionValue.getActualGenericTypeValues(),
                Optional.ofNullable(callCollector.functionValue));

        System.out.println("------ FunctionDeclarationResolver stats -------");
        System.out.println("1. originalFunctionResult");
        System.out.println(originalFunctionResult);
        System.out.println("2. resolvedFunctionResult");
        System.out.println(resolvedFunctionResult);
        System.out.println();

        return FunctionDeclarationResolveResult.of(originalFunctionResult, resolvedFunctionResult, TypeEvaluator.get(originalFunctionResult, resolvedFunctionResult));
    }

    /**
     * Internally calls {@link #findFunctionDeclaration} with the result being used to return the appropriate
     * {@code TypeEvaluator} should one exist.
     */
    public static FunctionDeclarationResolveResult functionDeclarationTypeEvaluator(@NotNull PsiElement originalElement, @NotNull PsiElement resolvedElement) {
        System.out.println("In FunctionDeclarationResolver functionDeclarationTypeEvaluator");
        return getTypeEvaluator(originalElement, findFunctionDeclaration(resolvedElement));
    }
}
