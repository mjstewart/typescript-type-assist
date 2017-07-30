package intentions.variableAssignment;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecma6.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import icons.PluginIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import settings.TypeAssistApplicationSettings;

import javax.swing.*;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * This intention assigns an object property to a local variable including type information.
 * The variable declaration such as const, let, var is determined by the 'variable declaration' in the settings menu.
 */
public class AssignVariableIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {
    private final Set<String> invalidReturnTypes = new HashSet<>(Arrays.asList("any", "void", "undefined", "null", "never"));


    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        JSExpressionStatement originalExpression = getExpression(psiElement);
        if (originalExpression == null) return;

        Caret caret = editor.getCaretModel().getCurrentCaret();
        Document document = editor.getDocument();

        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
        TextRange range = originalExpression.getTextRange();

        Consumer<String> writeCommand = value ->
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.deleteString(range.getStartOffset(), range.getEndOffset());
                    document.insertString(range.getStartOffset(), value);
                    caret.moveToOffset(range.getStartOffset() + value.length());
                });

        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(originalExpression, JSReferenceExpression.class);
        PsiElement resolvedElement = reference == null ? null : reference.resolve();
        if (resolvedElement == null) return;

        System.out.println("expression: " + originalExpression.getText());
        System.out.println("resolvedElement: " + resolvedElement.getText());
        System.out.println();

        if (resolvedElement instanceof TypeScriptPropertySignature) {
            System.out.println("resolvedElement instanceof TypeScriptPropertySignature");
            TypeScriptPropertySignature property = (TypeScriptPropertySignature) resolvedElement;

            String type = property.getType() == null ? "any" : property.getType().getTypeText();
            String assignment = varType + " " + property.getMemberName() + ": " + type + " = " + originalExpression.getText();
            writeCommand.accept(assignment);
            return;
        }

        if (resolvedElement instanceof TypeScriptVariable) {
            System.out.println("resolvedElement instanceof TypeScriptVariable");

            TypeScriptVariable variable = (TypeScriptVariable) resolvedElement;

            List<CallExpression> callExpressions = getCallExpressions(originalExpression);

            /*
             * If the resolved function has an explicit return type, use that, otherwise try find the inferred function
             * expression.
             *
             * const adder: (a: number, c: number) => (b: number) => number = (a: number) => (b: number): number => a + b;
             *               <---           explicit                   --->    <---      expression         --->
             */
            ResolvedFunctionTypes functionTypes = new ResolvedFunctionTypes(PsiTreeUtil.findChildrenOfAnyType(variable, TypeScriptFunctionType.class));
            ResolvedFunctionTypes functionExpression = new ResolvedFunctionTypes(PsiTreeUtil.findChildrenOfAnyType(variable, TypeScriptFunctionExpression.class));
            functionTypes = !functionTypes.isEmpty() ? functionTypes : functionExpression;

            System.out.println("--- Call expressions ---");
            callExpressions.forEach(System.out::println);

            System.out.println();

            System.out.println("--- Function types ---");
            functionTypes.functionTypes.forEach(System.out::println);

            System.out.println();
            System.out.println("--- Param pairings ---");
//            functionTypes.getLastReturnType(callExpressions);


            if (callExpressions.isEmpty()) {
                // Function call not invoked - missing call expression (). Assign the complete function return type.
//                String assignment = varType + " " + reference.getText() + ": " + functionTypes.get(0) + " = " + expression.getText();
//                writeCommand.accept(assignment);
                return;
            }


//                TypeScriptFunctionExpression functionExpression = PsiTreeUtil.findChildOfType(variable, TypeScriptFunctionExpression.class);
//                TypeScriptFunction function = functionType != null ? functionType : functionExpression;

//                if (function != null) {
//                    System.out.println("Function != null");
//                    TypeScriptTypeParameterList typeParameterList = function.getTypeParameterList();
//                    System.out.println(typeParameterList);
////                    for (TypeScriptTypeParameter typeParameter : typeParameterList.getTypeParameters()) {
////                        System.out.println("typeParam: " + typeParameter.getText());
////                    }
////                    System.out.println("returnType: " + function.getReturnTypeElement().getText());
//
//
//                    TypeScriptType returnTypeElement = function.getReturnTypeElement();
//                    String returnType = returnTypeElement == null ? "any" : returnTypeElement.getText();
//                    String assignment = varType + " " + reference.getText() + ": " + returnType + " = " + expression.getText();
//                    writeCommand.accept(assignment);
//                } else {
//                    TypeScriptSingleType singleType = PsiTreeUtil.findChildOfType(variable, TypeScriptSingleType.class);
//                    if (singleType == null) return;
//
//                    String type = singleType.getText() == null ? "any" : singleType.getText();
//                    String assignment = varType + " " + reference.getText() + ": " + type + " = " + expression.getText();
//                    writeCommand.accept(assignment);
//                }
            return;
        }

        if (resolvedElement instanceof TypeScriptFunction) {
            // isAvailable guarantees return type is valid and there is a call expression.

            System.out.println("resolvedElement instanceof TypeScriptFunction");
            TypeScriptFunction function = (TypeScriptFunction) resolvedElement;

            System.out.println("RETURN TYPE IS: " + function.getReturnType().getTypeText(JSType.TypeTextFormat.CODE));

            List<CallExpression> callExpressions = getCallExpressions(originalExpression);

            // <T, U>
            List<String> genericTypes = getGenericTypeParameters(function);
            // <string, number>
            List<String> typeParameterValues = getTypeParameterValues(callExpressions.get(0).jsCallExpression);
            // Given [T -> string, U, number], a pair is [\bT\b, string] which means find T and replace with string.
            List<Pair<String, String>> genericTypeReplacementPairs =
                    zipInto(genericTypes, typeParameterValues, (genericType, typeValue) ->
                            new GenericParameterPair(genericType, typeValue).getReplacementInstruction());

            if (function.getReturnType().getSource().getSourceElement() instanceof TypeScriptFunction) {
                System.out.println("-- genericTypes -- " + genericTypes);
                System.out.println("-- typeParameterValues -- " + typeParameterValues);
                System.out.println("-- genericTypeReplacementPairs -- " + genericTypeReplacementPairs);

                TypeScriptFunction typeScriptFunction = (TypeScriptFunction) function.getReturnType().getSource().getSourceElement();
                ResolvedFunctionTypes resolvedFunctionTypes = new ResolvedFunctionTypes(typeScriptFunction,
                        PsiTreeUtil.findChildrenOfAnyType(typeScriptFunction, TypeScriptFunction.class));

                System.out.println("resolvedFunctionTypes: " + resolvedFunctionTypes.functionTypes);

                // todo : dont actually need param pairings here. just for debugging.
                List<ParameterPairing> parameterPairings = resolvedFunctionTypes.createParameterPairings(callExpressions);
                System.out.println("param pairings: " + parameterPairings);
                System.out.println("all call exp: " + callExpressions);
                System.out.println();

                for (ParameterPairing parameterPairing : parameterPairings) {
                    System.out.println("pairing 1 = " + parameterPairing);
                }


                /*
                 * call expressoins.size == param pairings.size == fully applied, get last return statement.
                 * get return value
                 */
                String returnType = resolvedFunctionTypes.getReturnFunctionType(callExpressions, genericTypeReplacementPairs);

                System.out.println("THE RETURN TYPE SHOULD BE: " + returnType);

            } else {
                // single type?
                System.out.println("Single type return value = " + function.getReturnType().getTypeText(JSType.TypeTextFormat.CODE));


                for (JSParameterListElement jsParameterListElement : function.getParameterList().getParameters()) {
                    System.out.println("PARAM ELEMENT 1: " + jsParameterListElement.getText());
                    System.out.println("PARAM ELEMENT 2: " + jsParameterListElement.getTypeElement());
                }
                callExpressions.forEach(c -> {
                    System.out.println("Call EXP: " + c);
                });
            }



            /*
             * first element in call expression must match parameter list values.
             * The next call expression must match to next return type token.
             */


//            String returnType = function.getReturnTypeElement() == null ? "any" : function.getReturnTypeElement().getText();
//
//            String assignment = varType + " " + function.getName() + ": " + returnType + " = " + expression.getText();
//            writeCommand.accept(assignment);
        }
    }


    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (editor == null) return false;

        JSExpressionStatement expression = getExpression(psiElement);
        if (expression == null) return false;

        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(expression, JSReferenceExpression.class);
        PsiElement resolvedElement = reference == null ? null : reference.resolve();
        if (resolvedElement == null) return false;

        if (resolvedElement instanceof TypeScriptPropertySignature) {
            return true;
        }

        if (resolvedElement instanceof TypeScriptVariable) {
            TypeScriptVariable variable = (TypeScriptVariable) resolvedElement;
            // TODO: check if function is callable etc.
            return PsiTreeUtil.findChildOfType(variable, TypeScriptSingleType.class) != null;
        }

        if (resolvedElement instanceof TypeScriptFunction) {
            JSCallExpression callExpression = PsiTreeUtil.findChildOfType(expression, JSCallExpression.class);
            if (callExpression == null) return false;

            TypeScriptFunction function = (TypeScriptFunction) resolvedElement;
            if (function.getReturnType() == null) return false;
            String type = function.getReturnType().getTypeText(JSType.TypeTextFormat.CODE);
            return !invalidReturnTypes.contains(type);
        }
        return false;
    }

    @NotNull
    @Override
    public String getText() {
        return "Assign local typescript variable";
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public Icon getIcon(int i) {
        return PluginIcons.TS_ASSIST;
    }


//    /**
//     * <pre>
//     * genericParameterPairs: [T -> string, Y -> number]
//     * functionTypes: [(b: Y) => (c: T) => Y, (c: T) => Y]
//     *
//     * returns: [(b: number) => (c: string) => number, (c: string) => number]}
//     * </pre>
//     */
//    private List<String> fillGenericTypesWithActualValues(List<GenericParameterPair> genericParameterPairs,
//                                                  ResolvedFunctionTypes resolvedFunctionTypes) {
//        System.out.println("fillGenericTypesWithActualValues");
//        System.out.println("genericParameterPairs: " + genericParameterPairs);
//        System.out.println("functionTypes: " + resolvedFunctionTypes.functionTypes);
//
//        List<Pair<String, String>> replacements = genericParameterPairs.stream()
//                .map(GenericParameterPair::getReplacementInstruction)
//                .collect(Collectors.toList());
//
//        Function<FunctionType, String> fillTypeValues = functionType -> {
//            String filledTypeSignature = functionType.signature();
//            for (Pair<String, String> replacementPair : replacements) {
//                filledTypeSignature = filledTypeSignature.replaceAll(replacementPair.first, replacementPair.second);
//            }
//            return filledTypeSignature;
//        };
//
//        List<String> collect = resolvedFunctionTypes.functionTypes.stream()
//                .map(fillTypeValues)
//                .collect(Collectors.toList());
//
//        System.out.println("with generic values filled in");
//        collect.forEach(System.out::println);
//        System.out.println();
//        return collect;
//    }

    /**
     * {@code doStuff<T, U, V>} ....
     *
     * @return List of the type parameters such as List(T, U, V).
     */
    private List<String> getGenericTypeParameters(@NotNull TypeScriptFunction function) {
        if (function.getTypeParameterList() == null) return new ArrayList<>();
        return Arrays.stream(function.getTypeParameterList().getTypeParameters())
                .map(TypeScriptTypeParameter::getName)
                .collect(Collectors.toList());
    }

    /**
     * {@code doStuff<string, number, Cat>} ....
     *
     * @return List of the type parameters such as List(string, number, Cat).
     */
    private List<String> getTypeParameterValues(@NotNull JSCallExpression callExpression) {
        return PsiTreeUtil.findChildrenOfType(callExpression, TypeScriptTypeArgumentList.class).stream()
                .map(TypeScriptTypeArgumentList::getTypeArguments)
                .flatMap(Arrays::stream)
                .map(PsiElement::getText)
                .collect(Collectors.toList());
    }

    /**
     * Returns the transformed list containing the elements which result from applying the mapper to each corresponding
     * value in lists a and b..
     */
    private static <T, Y> List<Y> zipInto(List<T> a, List<T> b, BiFunction<T, T, Y> mapper) {
        return IntStream.range(0, Math.min(a.size(), b.size()))
                .mapToObj(i -> mapper.apply(a.get(i), b.get(i)))
                .collect(Collectors.toList());
    }

    /**
     * Find and replaces the {@code value} with the list of pairs containing the regex search key and replacement value.
     *
     * @return The new value with all tokens replaced with the new values.
     */
    private String replace(String value, List<Pair<String, String>> replacementPairs) {
        for (Pair<String, String> replacementPair : replacementPairs) {
            value = value.replaceAll(replacementPair.first, replacementPair.second);
        }
        return value;
    }

    /**
     * T -> string
     */
    private class GenericParameterPair {
        private String genericType;
        private String genericTypeValue;

        private GenericParameterPair(String genericType, String genericTypeValue) {
            this.genericType = genericType;
            this.genericTypeValue = genericTypeValue;
        }

        @Override
        public String toString() {
            return genericType + " -> " + genericTypeValue;
        }

        /**
         * @return Pair.1 contains the regex search key, Pair.2 is the replacement value.
         */
        private Pair<String, String> getReplacementInstruction() {
            return Pair.create(wordBoundary(genericType), genericTypeValue);
        }

        private String wordBoundary(String value) {
            return "\\b" + value + "\\b";
        }
    }


    private JSExpressionStatement getExpression(@NotNull PsiElement psiElement) {
        JSExpressionStatement parentExpression = PsiTreeUtil.getTopmostParentOfType(psiElement, JSExpressionStatement.class);
        if (parentExpression != null) return parentExpression;
        return (psiElement.getPrevSibling() instanceof JSExpressionStatement) ? (JSExpressionStatement) psiElement.getPrevSibling() : null;
    }

    /**
     *
     * Given a return type of {@code (a: A) => (b: B) => (c: C) => D}
     *
     * <p>ResolvedFunctionTypes contains a list of all the possible return values to cater for partially applied
     * function variations.</p>
     *
     * <pre>
     * (a: A) => (b: B) => (c: C) => D
     * (b: B) => (c: C) => D
     * (c: C) => D
     * </pre>
     *
     * If this example function is partially applied 3 times, the final return value is D.
     */
    private class ResolvedFunctionTypes {
        private List<FunctionType> functionTypes;

        /**
         * @param function            If supplied, is added as the value in index 0 in the {@code functionTypes} list.
         * @param typeScriptFunctions Any more function types which should be added to the {@code functionTypes} list.
         */
        private ResolvedFunctionTypes(TypeScriptFunction function,
                                      @NotNull Collection<TypeScriptFunction> typeScriptFunctions) {
            functionTypes = new ArrayList<>();
            if (function != null) {
                functionTypes.add(new FunctionType(function));
            }
            functionTypes.addAll(typeScriptFunctions.stream()
                    .map(FunctionType::new)
                    .collect(Collectors.toList()));
        }

        private ResolvedFunctionTypes(@NotNull Collection<TypeScriptFunction> typeScriptFunctions) {
            this(null, typeScriptFunctions);
        }

        private boolean isEmpty() {
            return functionTypes.isEmpty();
        }

        private FunctionType getLastFunction() {
            return functionTypes.get(functionTypes.size() - 1);
        }

        private List<ParameterPairing> createParameterPairings(List<CallExpression> callExpressions) {
            return IntStream.range(0, Math.min(functionTypes.size(), callExpressions.size()))
                    .mapToObj(i -> new ParameterPairing(functionTypes.get(i), callExpressions.get(i)))
                    .collect(Collectors.toList());
        }

        /**
         * This example explains how the correct return type is resolved.
         *
         * <pre>
         *  {@code function special<A, B, C, D>(a: A, b: B): (a: A) => (b: B) => (c: C) => D {
         *         ....
         *    }}
         *
         *   // The functionTypes are taken from the return type in this example.
         *   functionTypes = [(a: A) => (b: B) => (c: C) => D, (b: B) => (c: C) => D, (c: C) => D]
         *   index 0 - (a: A) => (b: B) => (c: C) => D
         *   index 1 - (b: B) => (c: C) => D
         *   index 2 - (c: C) => D
         *
         *  {@code special<string, string, number, boolean>("a", "b");}
         *   callExpressions = [("a", "b")]
         *
         * </pre>
         *
         * <p>Index 0 in callExpressions refers to the arguments supplied to the first function call, therefore
         * index 0 in functionTypes is the return value.</p>
         *
         * <p>If the returned function was called again, 2 elements would be in callExpressions and index 1 in
         * functionTypes is the return value.</p>
         *
         * <p>The special case is if the function is fully applied which means the last function with argument c
         * is invoked. This the function is fully applied, index 2 contains the final return type of D.</p>
         */
        private String getReturnFunctionType(List<CallExpression> callExpressions,
                                             List<Pair<String, String>> genericTypeReplacementPairs) {
            System.out.println("#### getReturnFunctionType: callExpressions = " + callExpressions);
            System.out.println("#### getReturnFunctionType: functionTypes = " + functionTypes);
            if (callExpressions.size() == functionTypes.size() + 1) {
                // function is fully applied.
                return replace(getLastFunction().returnType(), genericTypeReplacementPairs);
            }
            return replace(functionTypes.get(callExpressions.size() - 1).signature(), genericTypeReplacementPairs);
        }
    }

    private class FunctionType {
        private TypeScriptFunction typeScriptFunction;

        private FunctionType(TypeScriptFunction typeScriptFunction) {
            this.typeScriptFunction = typeScriptFunction;
        }

        private String parameters() {
            return typeScriptFunction.getParameterList() == null ? ""
                    : typeScriptFunction.getParameterList().getText();
        }

        private String returnType() {
            return typeScriptFunction.getReturnTypeElement() == null ? "any"
                    : typeScriptFunction.getReturnTypeElement().getText();
        }

        private String signature() {
            return parameters() + " => " + returnType();
        }

        @Override
        public String toString() {
            return signature();
        }
    }

    /**
     * Collects all {@code JSCallExpression}s and returns them in the reverse order to appear in the same order
     * as they are applied to the function invocations.
     *
     * <p>If a function returns curried functions it will look like this without reversing.</p>
     * <pre>
     *     {@code function doThis<A, B>(a: A, x: B): (b: B) => (c: A) => B}
     *
     *     {@code doThis<string, number>("#", "3")("4")}
     *
     *      callExpressions = [("4"), ("#", "3")] - in the opposite order as they are applied, so reverse fixes this.
     * </pre>
     */
    private List<CallExpression> getCallExpressions(JSExpressionStatement expression) {
        List<CallExpression> callExpressions = PsiTreeUtil.findChildrenOfType(expression, JSCallExpression.class)
                .stream().map(CallExpression::new)
                .collect(Collectors.toList());
        Collections.reverse(callExpressions);
        return callExpressions;
    }

    /**
     * A {@code CallExpression} is the function arguments such as {@code concat("hello", "world") where the
     * arguments are {@code ("hello", "world")}.
     */
    private class CallExpression {
        private JSCallExpression jsCallExpression;

        private CallExpression(JSCallExpression jsCallExpression) {
            this.jsCallExpression = jsCallExpression;
        }

        private String getArguments() {
            if (jsCallExpression.getArgumentList() == null) {
                return "";
            }
            return jsCallExpression.getArgumentList().getText();
        }

        @Override
        public String toString() {
            return getArguments();
        }
    }

    private class ParameterPairing {
        private FunctionType functionType;
        private CallExpression callExpression;

        private ParameterPairing(FunctionType functionType, CallExpression callExpression) {
            this.functionType = functionType;
            this.callExpression = callExpression;
        }

        @Override
        public String toString() {
            return callExpression + " --> " + functionType;
        }
    }
}