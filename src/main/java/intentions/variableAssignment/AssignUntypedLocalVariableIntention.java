package intentions.variableAssignment;

import com.intellij.codeInsight.highlighting.HighlightManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.psi.JSExpressionStatement;
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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import settings.TypeAssistApplicationSettings;

import javax.swing.*;
import java.util.function.Consumer;

public class AssignUntypedLocalVariableIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {
    private final String TEMP_VAR_PLACEHOLDER = "val";

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        JSExpressionStatement originalExpression = getExpression(psiElement);
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

        writeCommand.accept(createUntypedVarAssignment(originalExpression.getText()));
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {

        if (editor == null) return false;

        if (PsiTreeUtil.findChildOfType(psiElement, PsiErrorElement.class) != null) {
            return false;
        }

        JSExpressionStatement expression = getExpression(psiElement);
        return expression != null;
    }


    @NotNull
    @Override
    public String getText() {
        return "Assign to untyped local variable";
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

    private String createUntypedVarAssignment(String expression) {
        String varType = TypeAssistApplicationSettings.getInstance().VARIABLE_DECLARATION.getCode();
        String assignment = varType + " " + TEMP_VAR_PLACEHOLDER + " = " + expression;
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

    public JSExpressionStatement getExpression(@NotNull PsiElement psiElement) {
        JSExpressionStatement parentExpression = PsiTreeUtil.getParentOfType(psiElement, JSExpressionStatement.class);
        if (parentExpression != null) return parentExpression;
        return (psiElement.getPrevSibling() instanceof JSExpressionStatement) ? (JSExpressionStatement) psiElement.getPrevSibling() : null;
    }
}
