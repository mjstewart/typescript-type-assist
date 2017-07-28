package intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.psi.JSExpressionStatement;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.JSType;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptPropertySignature;
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

/**
 * This intention assigns an object property to a local variable including type information.
 * The variable declaration such as const, let, var is determined by the 'variable declaration' in the settings menu.
 */
public class AssignVariableIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {
    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        if (psiElement.getPrevSibling() instanceof JSExpressionStatement) {
            JSExpressionStatement expression = (JSExpressionStatement) psiElement.getPrevSibling();
            JSReferenceExpression reference = PsiTreeUtil.findChildOfType(expression, JSReferenceExpression.class);
            PsiElement resolvedElement = reference == null ? null : reference.resolve();

            if (resolvedElement instanceof TypeScriptPropertySignature) {
                TypeScriptPropertySignature property = (TypeScriptPropertySignature) resolvedElement;

                String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
                String type = property.getType() == null ? "any" : property.getType().getTypeText(JSType.TypeTextFormat.CODE);
                String assignment = varType + " " + property.getMemberName() + ": " + type + " = " + expression.getText();

                Caret caret = editor.getCaretModel().getCurrentCaret();
                Document document = editor.getDocument();

                WriteCommandAction.runWriteCommandAction(project, () -> {
                    TextRange range = expression.getTextRange();

                    document.deleteString(range.getStartOffset(), range.getEndOffset());
                    document.insertString(range.getStartOffset(), assignment);
                    caret.moveToOffset(range.getStartOffset() + assignment.length());
                });
            }
        }
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (editor == null) return false;

        if (psiElement.getPrevSibling() instanceof JSExpressionStatement) {
            JSExpressionStatement expression = (JSExpressionStatement) psiElement.getPrevSibling();
            JSReferenceExpression reference = PsiTreeUtil.findChildOfType(expression, JSReferenceExpression.class);
            PsiElement resolvedElement = reference == null ? null : reference.resolve();
            return (resolvedElement instanceof TypeScriptPropertySignature);
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
}