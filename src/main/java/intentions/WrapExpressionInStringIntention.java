package intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.psi.JSExpressionStatement;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import icons.PluginIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import settings.TypeAssistApplicationSettings;

import javax.swing.*;

/**
 * Given some expression, wrap it in a String based on {@code TypeAssistApplicationSettings.STRING_STYLE}.
 *
 * <p>In some cases the type is not always resolvable and the IDE highlights it red. This often happens when the type
 * is in the same file and is not being used via an import statement. This intention therefore provides another way
 * to try make a type resolvable and is similar to {@code ResolveArrayTypeIntention}</p>
 */
public class WrapExpressionInStringIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {

    private TypeAssistApplicationSettings settings = TypeAssistApplicationSettings.getInstance();

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        JSExpressionStatement expressionStatement = PsiTreeUtil.getTopmostParentOfType(psiElement, JSExpressionStatement.class);
        JSReferenceExpression referenceExpression = PsiTreeUtil.getTopmostParentOfType(psiElement, JSReferenceExpression.class);
        PsiElement element = expressionStatement == null ? referenceExpression : expressionStatement;
        if (element == null) return;

        // Exclude putting the semi colon in quotes if it exists.
        boolean endsWithSemiColon = element.getText().endsWith(";");
        String quotedValue = endsWithSemiColon ? element.getText().substring(0, element.getText().length() - 1) : element.getText();
        String endSemiColon = endsWithSemiColon ? ";" : "";

        String transformedCode = String.format("%s%s%s%s", settings.STRING_STYLE.getStyleToken(), quotedValue,
                settings.STRING_STYLE.getStyleToken(), endSemiColon);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            TextRange range = element.getTextRange();

            document.deleteString(range.getStartOffset(), range.getEndOffset());
            document.insertString(element.getTextRange().getStartOffset(), transformedCode);

            // The document must be commit before running the formatter.
            PsiDocumentManager.getInstance(project).commitDocument(document);

            // Place caret one space in from the last quote added.
            editor.getCaretModel().moveToOffset(range.getEndOffset() + 1);
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        if (editor == null) return false;

        JSExpressionStatement expressionStatement = PsiTreeUtil.getTopmostParentOfType(psiElement, JSExpressionStatement.class);
        JSReferenceExpression referenceExpression = PsiTreeUtil.getTopmostParentOfType(psiElement, JSReferenceExpression.class);
        return expressionStatement != null || referenceExpression != null;

    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @NotNull
    @Override
    public String getText() {
        return "Wrap expression in string";
    }


    @Override
    public Icon getIcon(int i) {
        return PluginIcons.TS_ASSIST;
    }
}
