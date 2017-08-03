package intentions.variableAssignment;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.psi.JSExpressionStatement;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.JSType;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptPropertySignature;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptVariable;
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
import intentions.variableAssignment.evaluators.FunctionTypeEvaluator;
import intentions.variableAssignment.evaluators.TypeEvaluator;
import intentions.variableAssignment.resolvers.FunctionDeclarationResolveResult;
import intentions.variableAssignment.resolvers.FunctionDeclarationResolver;
import intentions.variableAssignment.resolvers.ResolveUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import settings.TypeAssistApplicationSettings;
import utils.AppUtils;

import javax.swing.*;
import java.util.*;
import java.util.function.Consumer;

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

        JSExpressionStatement originalExpression = ResolveUtils.getExpression(psiElement);
        if (originalExpression == null) return;

        Caret caret = editor.getCaretModel().getCurrentCaret();
        Document document = editor.getDocument();
        TextRange range = originalExpression.getTextRange();

        Consumer<String> writeCommand = value ->
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.deleteString(range.getStartOffset(), range.getEndOffset());
                    document.insertString(range.getStartOffset(), value);
                    caret.moveToOffset(range.getStartOffset() + value.length());
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                    repositionCaret(project, document, editor, range);
                });

        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(originalExpression, JSReferenceExpression.class);
        PsiElement resolvedElement = reference == null ? null : reference.resolve();
        if (resolvedElement == null) return;

        System.out.println(" ################## START ####################################");
        System.out.println("originalExpression expression: " + originalExpression.getText());
        System.out.println("resolvedElement: " + resolvedElement.getText());
        System.out.println();

        if (resolvedElement instanceof TypeScriptPropertySignature) {
            TypeScriptPropertySignature property = (TypeScriptPropertySignature) resolvedElement;
            String returnType = property.getType() == null ? "any" : property.getType().getTypeText();
            writeCommand.accept(createVarAssignment(returnType, property.getName(), originalExpression.getText()));
            return;
        }

        if (resolvedElement instanceof TypeScriptVariable) {
            System.out.println("##########################   resolvedElement instanceof TypeScriptVariable");

            Optional<TypeEvaluator> evaluator = FunctionDeclarationResolver
                    .functionDeclarationTypeEvaluator(originalExpression, resolvedElement)
                    .getTypeEvaluator();

            if (evaluator.isPresent()) {
                System.out.println("TypeScriptVariable: evaluator found");
                writeCommand.accept(createVarAssignment(evaluator.get().evaluate(), originalExpression.getText()));
                return;
            }
            System.out.println("No action was done in TypeScriptVariable");
            return;
        }

        if (resolvedElement instanceof TypeScriptFunction) {
            System.out.println();
            System.out.println();
            System.out.println("resolvedElement instanceof TypeScriptFunction");


            TypeScriptFunction resolvedFunction = (TypeScriptFunction) resolvedElement;

//            Optional<OriginalFunctionResult> originalFunctionResult = OriginalFunctionResult.of(ResolveUtils.getCallExpressions(originalExpression));
//
//            // Any generic types of the resolved function such as <T, U>
//            List<String> genericTypeParameters = ResolveUtils.getGenericTypeParameters(resolvedFunction);
//
//            // There must be some type of function type or expression to the right of the function name.
//            Optional<ResolvedFunctionResult> resolvedFunctionResult = ResolvedFunctionResult.of(genericTypeParameters, ResolveUtils.getFunctionValue(resolvedFunction));
//
//            System.out.println("-------- optionalOriginalFunctionResult: ");
//            System.out.println(originalFunctionResult);
//            System.out.println("-------- resolvedFunctionResult: ");
//            System.out.println(resolvedFunctionResult);
//            System.out.println();

            FunctionDeclarationResolveResult resolveResult = FunctionDeclarationResolver
                    .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);

            if (resolveResult.getTypeEvaluator().isPresent()) {
                TypeEvaluator evaluator = resolveResult.getTypeEvaluator().get();
                writeCommand.accept(createVarAssignment(evaluator.evaluate(), originalExpression.getText()));
                return;
            }

//            // No function return type but the original and resolved function results can be used to extract any generic type info.
//            Optional<OriginalFunctionResult> originalFunctionResult = resolveResult.getOriginalFunctionResult();
//            Optional<ResolvedFunctionResult> resolvedFunctionResult = resolveResult.getResolvedFunctionResult();
//
//            // We now know that the resolvedFunction does not have a function type.
//            if (!originalFunctionResult.isPresent()) {
//                System.out.println("No call expressions and no function types - Occurs when a variable is assigned to an uncalled function");
//                // No call expressions and no function types - Occurs when a variable is assigned to an uncalled function
//                String params = resolvedFunction.getParameterList() == null ? "()" : resolvedFunction.getParameterList().getText();
//                String returnType = resolvedFunction.getReturnType() == null ? "any" : resolvedFunction.getReturnType().getTypeText(JSType.TypeTextFormat.CODE);
//                writeCommand.accept(createParameterAssignment(params, returnType, resolvedFunction.getName()));
//                return;
//            }
//
//            System.out.println("There are call expressions but the return type is something other than a function");
//            // There are call expressions but the return type is something other than a function.
//            // isAvailable checks for a missing return type but checking again to suppress ide errors
//            if (resolvedFunction.getReturnType() == null || !resolvedFunctionResult.isPresent()) return;
//
//            // Allows replacing generic types with their concrete values.
//            List<GenericParameterPair> genericParameters = AppUtils.zipInto(resolvedFunctionResult.get().getActualGenericTypeValues(),
//                    originalFunctionResult.get().getActualGenericTypeValues(), GenericParameterPair::new);
//
//            String returnType = TypeEvaluator.replace(ResolveUtils.getFunctionReturnType(resolvedFunction), genericParameters);
//            writeCommand.accept(createVarAssignment(returnType, originalExpression.getText()));

        }

        System.out.println("---------------- nothing was done");
    }


    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (editor == null) return false;

        if (PsiTreeUtil.findChildOfType(psiElement, PsiErrorElement.class) != null) {
            return false;
        }

        JSExpressionStatement expression = ResolveUtils.getExpression(psiElement);
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
//     * Indirect function just means the {@code originalElement} variable is assigned to a variable that could be
//     * referring to a function. In which case we want to know the full type by resolving until we hit a concrete
//     * function declaration that can tell us the type information.
//     *
//     * <p>The {@code resolvedElement} can either be a {@code TypeScriptFunction} or {@code TypeScriptFunctionExpression}
//     * otherwise an empty {@code Optional} will be returned.</p>
//     *
//     * <p>First check if the {@code resolvedElement} is a function expression such as a lambda. If its not then use the
//     * {@code FunctionDeclarationResolver} to recurse up to try find the original function declaration.</p>
//     */
//    private Optional<TypeEvaluator> tryResolveIndirectFunction(@NotNull PsiElement originalElement, @NotNull PsiElement resolvedElement) {
//        /*
//         * Append call expressions from the original element to those of the resolvedElement. This rebuilds the list
//         * of all partially applied function calls should any exist. The total number of function calls is what
//         * determines the return type hence this must account for all calls. This is done to account for a partially
//         * applied lambda.
//         */
//        System.out.println("in tryResolveIndirectFunction");
//        System.out.println("originalElement: " + originalElement.getText());
//        System.out.println("resolvedElement: " + resolvedElement.getText());
//
////        List<CallExpressionWithArgs> callExpressions = ResolveUtils.getCallExpressions(resolvedElement);
////        callExpressions.addAll(ResolveUtils.getCallExpressions(originalElement));
////
////        Optional<OriginalFunctionResult> originalFunctionResult = OriginalFunctionResult.of(callExpressions);
////
////        Optional<ResolvedFunctionResult> resolvedFunctionResult =
////                ResolvedFunctionResult.of(ResolveUtils.getGenericTypeParameters(resolvedElement), ResolveUtils.getFunctionExpression(resolvedElement));
//
//        return FunctionDeclarationResolver.functionDeclarationTypeEvaluator(originalElement, resolvedElement).getTypeEvaluator();
//
////        // resolvedElement is a function expression such as a lambda.
////        return resolvedFunctionResult.<Optional<TypeEvaluator>>
////                map(result -> Optional.of(new FunctionTypeEvaluator(originalFunctionResult, result)))
////                .orElse(FunctionDeclarationResolver.functionDeclarationTypeEvaluator(originalElement, resolvedElement).getTypeEvaluator());
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
        return createVarAssignment(returnType, TEMP_VAR_PLACEHOLDER, expression);
    }

    private String createVarAssignment(String returnType, String variableName, String expression) {
        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
        String assignment = varType + " " + variableName + ": " + returnType + " = " + expression;
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
     * Formats the expression based on {@code TypeAssistApplicationSettings} end with semi colon option.
     */
    private String terminateExpression(String expression) {
        if (TypeAssistApplicationSettings.getInstance().END_WITH_SEMI_COLON) {
            return expression.endsWith(";") ? expression : expression + ";";
        }
        return expression.endsWith(";") ? expression.substring(0, expression.length() - 1) : expression;
    }

}