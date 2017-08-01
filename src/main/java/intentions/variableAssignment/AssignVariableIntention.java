package intentions.variableAssignment;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecma6.*;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import icons.PluginIcons;
import intentions.variableAssignment.evaluators.AnonymousFunctionEvaluator;
import intentions.variableAssignment.evaluators.ReturnTypeEvaluator;
import intentions.variableAssignment.evaluators.StandardFunctionEvaluator;
import intentions.variableAssignment.evaluators.FullTypeFunctionEvaluator;
import intentions.variableAssignment.functions.FunctionExpression;
import intentions.variableAssignment.functions.FunctionValue;
import intentions.variableAssignment.functions.StandardFunction;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import settings.TypeAssistApplicationSettings;
import utils.AppUtils;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

            /*
             * When the resolved variable is a variable, it can be assigned to 4 different expressions.
             *
             *               [      TypeScriptFunctionExpression        ]     No explicit type signature
             * const addMe = (a: number) => (b: number): number => a + b;
             *
             *           [       TypeScriptFunctionType                     ]
             * const me: (a: string) => (b: string) => (c: number) => boolean = special<string, string, number, boolean>("a", "b");
             *
             * // p4 is assigned to a JSCallExpression to the function print4, therefore the call is the type of p4.
             * // The user doesn't need to put type info in, but if you create another variable and assign it to p4, this intention puts type info in.
             * const p4 = print4<string, number>()("4", 5)("sss")
             * p4
             *
             * // single type
             * const hi = 'hhhhiii'
             *
             */

/**
 * https://github.com/JetBrains/intellij-community/blob/master/java/java-impl/src/com/intellij/codeInsight/intention/impl/IntroduceVariableIntentionAction.java
 * <p>
 * This intention assigns an object property to a local variable including type information.
 * The variable declaration such as const, let, var is determined by the 'variable declaration' in the settings menu.
 */
public class AssignVariableIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {
    private final Set<String> invalidReturnTypes = new HashSet<>(Arrays.asList("any", "void", "undefined", "null", "never"));
    private final String TEMP_VAR_PLACEHOLDER = "val";


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
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                });

        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(originalExpression, JSReferenceExpression.class);
        PsiElement resolvedElement = reference == null ? null : reference.resolve();
        if (resolvedElement == null) return;

        System.out.println(" ################## START ####################################");
        System.out.println("originalExpression expression: " + originalExpression.getText());
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
            System.out.println("##########################   resolvedElement instanceof TypeScriptVariable");

            TypeScriptVariable resolvedTypescriptVariable = (TypeScriptVariable) resolvedElement;

            List<FunctionValue> functionValues = getFunctionValues(resolvedTypescriptVariable);
            if (!functionValues.isEmpty()) {

                // TypescriptFunctionExpression section
                System.out.println("MUST BE LOOKING FOR functionTypes or functionExpression");

                Optional<OriginalFunctionResult> originalFunctionResult = OriginalFunctionResult.of(getCallExpressions(originalExpression));
                Optional<ResolvedFunctionResult> resolvedFunctionResult = ResolvedFunctionResult.of(functionValues);

                System.out.println("----- originalFunctionResult");
                System.out.println(originalFunctionResult);
                System.out.println("----- resolvedFunctionResult");
                System.out.println(resolvedFunctionResult);
                System.out.println();

                if (!resolvedFunctionResult.isPresent()) {
                    System.out.println("resolvedFunctionResult is empty");
                    // The resolved variable has no function expression so just assign the new variable to the resolved function.
                    writeCommand.accept(createUntypedVarAssignment(resolvedTypescriptVariable.getName()));
                    repositionCaret(project, document, editor, range);
                    return;
                }

                if (!originalFunctionResult.isPresent()) {
                    System.out.println("originalFunctionResult is empty");
                    // The original variable is not invoking the resolved function, therefore assigned the full type.
                    String returnType = new FullTypeFunctionEvaluator(resolvedFunctionResult.get()).evaluate();
                    System.out.println("RETURN TYPE: " + returnType);
                    writeCommand.accept(createVarAssignment(returnType, resolvedTypescriptVariable.getName()));
                    repositionCaret(project, document, editor, range);
                    return;
                }


                String returnType = new AnonymousFunctionEvaluator(originalFunctionResult.get(), resolvedFunctionResult.get()).evaluate();
                System.out.println("******  RETURN TYPE should be: " + returnType);


//                String returnType = functionTypes.getReturnType(originalCallExpressions);
//                System.out.println("Return type should be....: " + returnType);
//                writeCommand.accept(createVarAssignment(returnType, resolvedTypescriptVariable.getName() + toCallExpressionCode(originalCallExpressions)));
//                repositionCaret(project, document, editor, range);
                return;
            }

            // If there are no functions, check for other possible types.
            // const p4 = print4<string, number>()("4", 5)("sss") - variable assigned to call expression.
            System.out.println("Function types is empty, checking if variable is assigned to a JSCallExpression");

            // const p4 = print<string, number>()("4", 5)("sss") -> [(4, 5), (ssss)]
            Optional<OriginalFunctionResult> originalFunctionOptional =
                    OriginalFunctionResult.of(getCallExpressions(resolvedTypescriptVariable));

            if (originalFunctionOptional.isPresent()) {
                OriginalFunctionResult originalFunctionResult = originalFunctionOptional.get();


                Optional<ResolvedFunctionResult> resolvedFunctionResult = Optional.of(originalFunctionResult.getRootCallExpression())
                        .flatMap(callExpression -> Optional.ofNullable(PsiTreeUtil.findChildOfType(callExpression, JSReferenceExpression.class))
                                .flatMap(ref -> Optional.ofNullable(ref.resolve()))
                                .filter(resolved -> (resolved instanceof TypeScriptFunction))
                                .map(resolved -> (TypeScriptFunction) resolved)
                                .flatMap(function -> ResolvedFunctionResult.of(getGenericTypeParameters(function), getFunctionValues(function))));


//                Optional<ResolvedFunctionResult> resolvedFunctionResult = Optional.of(originalFunctionResult.getRootCallExpression())
//                        .flatMap(callExpression -> Optional.ofNullable(PsiTreeUtil.findChildOfType(callExpression, JSReferenceExpression.class))
//                                .flatMap(ref -> Optional.ofNullable(ref.resolve()))
//                                .filter(resolved -> (resolved instanceof TypeScriptFunction))
//                                .map(resolved -> (TypeScriptFunction) resolved)
//                                .map(function -> ResolvedFunctionResult.of(getGenericTypeParameters(function), getResolvedStandardFunctions(function))));

                System.out.println("RESULT  SO FAR?");
                System.out.println("-------- originalFunctionResult: ");
                System.out.println(originalFunctionResult);
                System.out.println("-------- resolvedFunctionResult: ");
                System.out.println(resolvedFunctionResult);
                System.out.println();
                new ReturnTypeEvaluator2(originalFunctionResult, resolvedFunctionResult.get()).evaluate();


//                        .flatMap(callExpression -> Optional.ofNullable(PsiTreeUtil.findChildOfType(callExpression, JSReferenceExpression.class))
//                                .flatMap(ref -> Optional.ofNullable(ref.resolve()))
//                                .flatMap(resolved -> toFunctionComponents(getActualGenericTypeValues(callExpression), resolved)));


//                System.out.println("---------- functionComponents");
//                System.out.println(functionComponents2);
//                System.out.println();
//
////                Optional<String> resolvedReturnValue = Optional.ofNullable(PsiTreeUtil.findChildOfType(variable, JSCallExpression.class))
////                        .flatMap(callExpression -> Optional.ofNullable(PsiTreeUtil.findChildOfType(callExpression, JSReferenceExpression.class))
////                                .flatMap(ref -> Optional.ofNullable(ref.resolve()))
////                                .flatMap(resolved -> toFunctionComponents(getActualGenericTypeValues(callExpression), resolved)))
////                        .map(functionComponents -> functionComponents.getFullyResolvedReturnType(this::replace));
//
//                if (resolvedReturnValue.isPresent()) {
//                    writeCommand.accept(createVarAssignment(resolvedReturnValue.get(), variable.getName()));
//                    repositionCaret(project, document, editor, range);
//                    return;
//                }
//
//                System.out.println("------------- Function resolvedReturnValue is: ");
//                System.out.println(resolvedReturnValue);
            }


            System.out.println("Call expression is empty, must be a single type or literal type");
            // Must be the simplest cases of just a single type.
            TypeScriptSingleType singleType = PsiTreeUtil.findChildOfType(resolvedTypescriptVariable, TypeScriptSingleType.class);
            if (singleType != null) {
                writeCommand.accept(createVarAssignment(singleType.getText(), resolvedTypescriptVariable.getName()));
                repositionCaret(project, document, editor, range);
                return;
            }

            JSLiteralExpression literalExpression = PsiTreeUtil.findChildOfType(resolvedTypescriptVariable, JSLiteralExpression.class);
            if (literalExpression != null) {
                writeCommand.accept(createUntypedVarAssignment(resolvedTypescriptVariable.getName()));
                repositionCaret(project, document, editor, range);
                return;
            }
            return;


        }

        if (resolvedElement instanceof TypeScriptFunction) {
            System.out.println("resolvedElement instanceof TypeScriptFunction");
            TypeScriptFunction resolvedFunction = (TypeScriptFunction) resolvedElement;

            // isAvailable does check for a call expression to exist, but double check here anyway.
            Optional<OriginalFunctionResult> originalFunctionResult = OriginalFunctionResult.of(getCallExpressions(originalExpression));

            // Any generic types of the resolved function such as <T, U>
            List<String> genericTypeParameters = getGenericTypeParameters(resolvedFunction);

            // There must be some type of function type or expression to the right of the function name.
            Optional<ResolvedFunctionResult> resolvedFunctionResult = ResolvedFunctionResult.of(genericTypeParameters, getFunctionValues(resolvedFunction));

            System.out.println("-------- originalFunctionResult: ");
            System.out.println(originalFunctionResult);
            System.out.println("-------- resolvedFunctionResult: ");
            System.out.println(resolvedFunctionResult);
            System.out.println();

            if (!originalFunctionResult.isPresent() && !resolvedFunctionResult.isPresent()) {
                System.out.println("No call expressions and no function types - Occurs when a variable is assigned to an uncalled function");
                // No call expressions and no function types - Occurs when a variable is assigned to an uncalled function
                String params = resolvedFunction.getParameterList() == null ? "()" : resolvedFunction.getParameterList().getText();
                String returnType = resolvedFunction.getReturnType() == null ? "any" : resolvedFunction.getReturnType().getTypeText(JSType.TypeTextFormat.CODE);
                writeCommand.accept(createParameterAssignment(params, returnType, resolvedFunction.getName()));
                repositionCaret(project, document, editor, range);
                return;
            }

            if (!originalFunctionResult.isPresent() && resolvedFunctionResult.isPresent()) {
                System.out.println("No call expressions but its still possible for return values.");
                // No call expressions but has function return types
                String returnType = new FullTypeFunctionEvaluator(resolvedFunctionResult.get()).evaluate();
                writeCommand.accept(createVarAssignment(returnType, originalExpression.getText()));
                repositionCaret(project, document, editor, range);
                return;
            }

            if (originalFunctionResult.isPresent() && resolvedFunctionResult.isPresent()) {
                // Call expressions with function return types
                System.out.println("BOTH call expressions and function values");

                String returnType = new StandardFunctionEvaluator(originalFunctionResult.get(), resolvedFunctionResult.get()).evaluate();
                writeCommand.accept(createVarAssignment(returnType, originalExpression.getText()));
                repositionCaret(project, document, editor, range);
                return;
            }

            System.out.println("There are call expressions but the return type is something other than a function");
            // There are call expressions but the return type is something other than a function.
            // isAvailable checks for a missing return type but checking again to suppress ide errors
            if (resolvedFunction.getReturnType() == null) return;
            List<GenericParameterPair> genericParameters = AppUtils.zipInto(genericTypeParameters,
                    originalFunctionResult.get().getActualGenericTypeValues(), GenericParameterPair::new);

            String returnType = ReturnTypeEvaluator.replace(resolvedFunction.getReturnType().getTypeText(JSType.TypeTextFormat.CODE), genericParameters);
            writeCommand.accept(createVarAssignment(returnType, originalExpression.getText()));
            repositionCaret(project, document, editor, range);
        }
    }


    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (editor == null) return false;

        if (PsiTreeUtil.findChildOfType(psiElement, PsiErrorElement.class) != null) {
            return false;
        }

        JSExpressionStatement expression = getExpression(psiElement);
        if (expression == null) return false;

        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(expression, JSReferenceExpression.class);
        PsiElement resolvedElement = reference == null ? null : reference.resolve();
        if (resolvedElement == null) return false;

        if (resolvedElement instanceof TypeScriptPropertySignature) {
            return true;
        }

        if (resolvedElement instanceof TypeScriptVariable) {
            return true;
        }

        if (resolvedElement instanceof TypeScriptFunction) {
//            JSCallExpression callExpression = PsiTreeUtil.findChildOfType(expression, JSCallExpression.class);
//            if (callExpression == null) return false;

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


//    private Optional<FunctionComponents> toFunctionComponents(@NotNull List<String> genericTypeActualValues, PsiElement psiElement) {
//        System.out.println("toFunctionComponents");
//        if (psiElement instanceof TypeScriptFunction) {
//            System.out.println("toFunctionComponents psiElement instanceof TypeScriptFunction");
//            return Optional.of(collectFunctionComponents(genericTypeActualValues, (TypeScriptFunction) psiElement));
//        }
//        return Optional.empty();
//    }
//
//    /**
//     *
//     * @param genericTypeActualValues If the generic types are [T, U], then this list will contain what the concrete
//     *                               types are such as [string, number] that will be substituted into the corresponding
//     *                               positions in the resolved function,
//     * @param function The function in which to collect all the components from.
//     */
//    private FunctionComponents collectFunctionComponents(@NotNull List<String> genericTypeActualValues,
//                                                         @NotNull TypeScriptFunction function) {
//        List<String> genericTypes = getGenericTypeParameters(function);
//
//        List<Pair<String, String>> genericTypeReplacementPairs =
//                AppUtils.zipInto(genericTypes, genericTypeActualValues, (genericType, typeValue) ->
//                        new GenericParameterPair(genericType, typeValue).getReplacementInstruction());
//
//        return new FunctionComponents(genericTypes, genericTypeActualValues, genericTypeReplacementPairs, getResolvedStandardFunctions(function));
//    }

    private void highlight(Project project, Editor editor, TextRange textRange) {
        TextAttributes textAttributes = new TextAttributes();
        TypeAssistApplicationSettings settings = TypeAssistApplicationSettings.getInstance();
        textAttributes.setEffectType(settings.PROPERTY_HIGHLIGHT_STYLE.getEffectType());
        textAttributes.setEffectColor(settings.toColor(settings.PROPERTY_HIGHLIGHT_HEX_COLOR));

        HighlightManager.getInstance(project).addRangeHighlight(editor, textRange.getStartOffset(), textRange.getEndOffset(),
                textAttributes, true, true, null);
    }

    /**
     * Moves the caret to the start of the temp variable name and highlights it ready for user to input new name.
     *
     * @param range The range of the original expression before variable assignment. This provides the starting point
     *              from which to advance the caret past the 'const/let' modifier to reach the var name.
     */
    private void repositionCaret(Project project, Document document, Editor editor, TextRange range) {
        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
        if (psiFile == null) return;
        PsiElement tempVarName = psiFile.findElementAt(range.getStartOffset() + varType.length() + 1);
        if (tempVarName == null) return;
        editor.getCaretModel().moveToOffset(range.getStartOffset() + varType.length() + 1);
        highlight(project, editor, tempVarName.getTextRange());
    }

    private String createVarAssignment(String returnType, String expression) {
        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
        String assignment = varType + " " + TEMP_VAR_PLACEHOLDER + ": " + returnType + " = " + expression;
        return terminateExpression(assignment);
    }

    private String createUntypedVarAssignment(String expression) {
        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
        String assignment = varType + " " + TEMP_VAR_PLACEHOLDER + " = " + expression;
        return terminateExpression(assignment);
    }

    private String createParameterAssignment(String parameters, String returnType, String expression) {
        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
        String assignment = varType + " " + TEMP_VAR_PLACEHOLDER + ": " + parameters + " => " + returnType + " = " + expression;
        return terminateExpression(assignment);
    }

    /**
     * Formats the expression based on {@code TypeAssistApplicationSettings} end with semi colon option,
     */
    private String terminateExpression(String expression) {
        if (TypeAssistApplicationSettings.getInstance().END_WITH_SEMI_COLON) {
            return expression.endsWith(";") ? expression : expression + ";";
        }
        return expression.endsWith(";") ? expression.substring(0, expression.length() - 1) : expression;
    }

    /**
     * [(...), (...), (...)] converts into (...)(...)(...)
     */
    private String toCallExpressionCode(List<CallExpressionWithArgs> callExpressions) {
        return callExpressions.stream().map(CallExpressionWithArgs::getArguments).collect(Collectors.joining(""));
    }

    /**
     * {@code doStuff<T, U, V>} ....
     *
     * @return List of the type parameters such as List(T, U, V).
     */
    private List<String> getGenericTypeParameters(@NotNull TypeScriptFunction function) {
        return Optional.ofNullable(function.getTypeParameterList())
                .map(TypeScriptTypeParameterList::getTypeParameters)
                .map(Arrays::stream)
                .map(s -> s.map(TypeScriptTypeParameter::getName))
                .map(params -> params.collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    /**
     * {@code doStuff<string, number, Cat>} ....
     * <p>
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
    private List<String> getActualGenericTypeValues(@NotNull JSCallExpression callExpression) {
        return PsiTreeUtil.findChildrenOfType(callExpression, TypeScriptTypeArgumentList.class).stream()
                .map(TypeScriptTypeArgumentList::getTypeArguments)
                .flatMap(Arrays::stream)
                .map(PsiElement::getText)
                .collect(Collectors.toList());
    }


    private JSExpressionStatement getExpression(@NotNull PsiElement psiElement) {
        JSExpressionStatement parentExpression = PsiTreeUtil.getTopmostParentOfType(psiElement, JSExpressionStatement.class);
        if (parentExpression != null) return parentExpression;
        return (psiElement.getPrevSibling() instanceof JSExpressionStatement) ? (JSExpressionStatement) psiElement.getPrevSibling() : null;
    }

    /**
     * Collects all {@code JSCallExpression}s and returns them in the reverse order to appear in the same order
     * as they are applied to the function invocations.
     * <p>
     * <p>If a function returns curried functions it will look like this without reversing.</p>
     * <pre>
     *     {@code function doThis<A, B>(a: A, x: B): (b: B) => (c: A) => B}
     *
     *     {@code doThis<string, number>("#", "3")("4")}
     *
     *      callExpressions = [("4"), ("#", "3")] - in the opposite order as they are applied, so reverse fixes this.
     * </pre>
     */
    private List<CallExpressionWithArgs> getCallExpressions(PsiElement expression) {
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

    private Optional<ResolvedFunctionTypes> getResolvedStandardFunctions(TypeScriptFunction function) {
        if (function.getReturnType() != null &&
                function.getReturnType().getSource().getSourceElement() instanceof TypeScriptFunction) {
            TypeScriptFunction typeScriptFunction = (TypeScriptFunction) function.getReturnType().getSource().getSourceElement();
            ResolvedFunctionTypes resolvedFunctionTypes = new ResolvedStandardFunctions(typeScriptFunction,
                    PsiTreeUtil.findChildrenOfAnyType(typeScriptFunction, TypeScriptFunction.class));
            return Optional.of(resolvedFunctionTypes);
        }
        return Optional.empty();
    }


    private List<FunctionValue> getFunctionValues(PsiElement element) {
        List<FunctionValue> standardFunctions = PsiTreeUtil.findChildrenOfAnyType(element, TypeScriptFunctionType.class).stream()
                .map(StandardFunction::new)
                .collect(Collectors.toList());
        if (!standardFunctions.isEmpty()) return standardFunctions;

        return PsiTreeUtil.findChildrenOfAnyType(element, TypeScriptFunctionExpression.class).stream()
                .map(FunctionExpression::new).collect(Collectors.toList());
    }
}