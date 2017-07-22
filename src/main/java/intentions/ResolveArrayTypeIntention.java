package intentions;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.psi.JSIndexedPropertyAccessExpression;
import com.intellij.lang.javascript.psi.JSLiteralExpression;
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
 * <p>In some cases the type is not always resolvable and the IDE highlights it red. This often happens when the type
 * is in the same file and is not being used via an import statement.</p>
 *
 * <p>This intention wraps the array type in a string which makes it resolvable (if it exists) which allows the
 * {@code CreateTypeScriptObjectAction} to be invoked.</p>
 *
 * <pre>
 *     Book[3] (not resolvable)
 *     'Book'[3] (now resolvable which allows other typescript actions to run)
 * </pre>
 */
public class ResolveArrayTypeIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {

    private TypeAssistApplicationSettings settings = TypeAssistApplicationSettings.getInstance();

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        // Find the expression, is the caret in the expression or is it 1 space after?
        JSIndexedPropertyAccessExpression indexedProperty = PsiTreeUtil.getTopmostParentOfType(psiElement, JSIndexedPropertyAccessExpression.class);
        if (indexedProperty == null) return;

        JSReferenceExpression referenceExpression = PsiTreeUtil.findChildOfType(indexedProperty, JSReferenceExpression.class);
        JSLiteralExpression literalExpression = PsiTreeUtil.findChildOfType(indexedProperty, JSLiteralExpression.class);
        if (referenceExpression == null) return;

        String indexedValue = literalExpression == null ? "[]" : "[" + literalExpression.getText() + "]";

        String transformedCode = String.format("%s%s%s%s", settings.STRING_STYLE.getStyleToken(), referenceExpression.getText(),
                settings.STRING_STYLE.getStyleToken(), indexedValue);

        WriteCommandAction.runWriteCommandAction(project, () -> {
            Document document = editor.getDocument();
            TextRange range = indexedProperty.getTextRange();

            document.deleteString(range.getStartOffset(), range.getEndOffset());
            document.insertString(indexedProperty.getTextRange().getStartOffset(), transformedCode);

            // The document must be commit before running the formatter.
            PsiDocumentManager.getInstance(project).commitDocument(document);

            // Advance caret 1 space to enable CreateTypeScriptObjectAction to be invokable if type is resolvable.
            editor.getCaretModel().moveToOffset(range.getStartOffset() + 1);
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        return editor != null &&
                PsiTreeUtil.getTopmostParentOfType(psiElement, JSIndexedPropertyAccessExpression.class) != null;
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
        return "Transform into resolvable typescript array form";
    }


    @Override
    public Icon getIcon(int i) {
        return PluginIcons.TS_ASSIST;
    }
}
