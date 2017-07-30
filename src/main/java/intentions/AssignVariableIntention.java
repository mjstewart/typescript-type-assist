package intentions;

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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import icons.PluginIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import settings.TypeAssistApplicationSettings;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * This intention assigns an object property to a local variable including type information.
 * The variable declaration such as const, let, var is determined by the 'variable declaration' in the settings menu.
 */
public class AssignVariableIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        JSExpressionStatement expression = getExpression(psiElement);
        if (expression == null) return;

        Caret caret = editor.getCaretModel().getCurrentCaret();
        Document document = editor.getDocument();

        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
        TextRange range = expression.getTextRange();

        Consumer<String> writeCommand = value ->
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.deleteString(range.getStartOffset(), range.getEndOffset());
                    document.insertString(range.getStartOffset(), value);
                    caret.moveToOffset(range.getStartOffset() + value.length());
                });

        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(expression, JSReferenceExpression.class);
        PsiElement resolvedElement = reference == null ? null : reference.resolve();
        if (resolvedElement == null) return;

        System.out.println("expression: " + expression.getText());
        System.out.println("resolvedElement: " + resolvedElement.getText());
        System.out.println();

        if (resolvedElement instanceof TypeScriptPropertySignature) {
            System.out.println("resolvedElement instanceof TypeScriptPropertySignature");
            TypeScriptPropertySignature property = (TypeScriptPropertySignature) resolvedElement;

            String type = property.getType() == null ? "any" : property.getType().getTypeText();
            String assignment = varType + " " + property.getMemberName() + ": " + type + " = " + expression.getText();
            writeCommand.accept(assignment);
            return;
        }

        if (resolvedElement instanceof TypeScriptVariable) {
            System.out.println("resolvedElement instanceof TypeScriptVariable");

            TypeScriptVariable variable = (TypeScriptVariable) resolvedElement;

            List<CallExpression> callExpressions = getCallExpressions(expression);

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
            functionTypes.getReturnType(callExpressions);


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
            System.out.println("resolvedElement instanceof TypeScriptFunction");
            TypeScriptFunction function = (TypeScriptFunction) resolvedElement;
            String returnType = function.getReturnTypeElement() == null ? "any" : function.getReturnTypeElement().getText();

            String assignment = varType + " " + function.getName() + ": " + returnType + " = " + expression.getText();
            writeCommand.accept(assignment);
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
            return PsiTreeUtil.findChildOfType(variable, TypeScriptSingleType.class) != null;
        }
        return resolvedElement instanceof TypeScriptFunction;
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


    private JSExpressionStatement getExpression(@NotNull PsiElement psiElement) {
        JSExpressionStatement parentExpression = PsiTreeUtil.getTopmostParentOfType(psiElement, JSExpressionStatement.class);
        if (parentExpression != null) return parentExpression;
        return (psiElement.getPrevSibling() instanceof JSExpressionStatement) ? (JSExpressionStatement) psiElement.getPrevSibling() : null;
    }


    private class ResolvedFunctionTypes {
        private List<FunctionType> functionTypes;

        private ResolvedFunctionTypes(@NotNull Collection<TypeScriptFunction> typeScriptFunctions) {
            functionTypes = typeScriptFunctions.stream()
                    .map(FunctionType::new)
                    .collect(Collectors.toList());
        }

        private boolean isEmpty() {
            return functionTypes.isEmpty();
        }

        private String getReturnType(List<CallExpression> callExpressions) {
            List<ParameterPairing> pairings = createParameterPairings(callExpressions);
            pairings.forEach(System.out::println);
            if (pairings.isEmpty()) return "NO RETURN TYPE";
            ParameterPairing lastCallPairing = pairings.get(pairings.size() - 1);
            lastCallPairing.isValid();
            return "";
        }


        private List<ParameterPairing> createParameterPairings(List<CallExpression> callExpressions) {
            int min = Math.min(functionTypes.size(), callExpressions.size());
            return IntStream.range(0, min)
                    .mapToObj(i -> new ParameterPairing(functionTypes.get(i), callExpressions.get(i)))
                    .collect(Collectors.toList());
        }

        private class ParameterPairing {
            private FunctionType functionType;
            private CallExpression callExpression;

            private ParameterPairing(FunctionType functionType, CallExpression callExpression) {
                this.functionType = functionType;
                this.callExpression = callExpression;
            }

            private boolean isValid() {
                System.out.println("functionType.typeScriptFunction.getTypeParameterList(): " + functionType.typeScriptFunction.getTypeParameterList());

                for (JSParameter jsParameter : functionType.typeScriptFunction.getParameterVariables()) {
                    System.out.println("PARAM: " + jsParameter.getType().getTypeText(JSType.TypeTextFormat.CODE));
                }
                return false;
            }

            @Override
            public String toString() {
                return callExpression + " --> " + functionType;
            }
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


    private List<CallExpression> getCallExpressions(JSExpressionStatement expression) {
        List<CallExpression> callExpressions = PsiTreeUtil.findChildrenOfType(expression, JSCallExpression.class)
                .stream().map(CallExpression::new)
                .collect(Collectors.toList());
        Collections.reverse(callExpressions);
        return callExpressions;
    }

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



}