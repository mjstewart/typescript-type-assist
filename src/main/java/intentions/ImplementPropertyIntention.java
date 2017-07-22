package intentions;

import codeInsight.TypeAssistPsiUtil;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import icons.PluginIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import settings.TypeAssistApplicationSettings;

import javax.swing.*;


/**
 * Allows user to remove placeholder property value so they can just start typing the implementation.
 * This is more of a UX feature.
 *
 * Created by matt on 26-May-17.
 */
public class ImplementPropertyIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {

    @NotNull
    @Override
    public String getText() {
        return "Implement typescript property value";
    }


    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        Caret caret = editor.getCaretModel().getCurrentCaret();
        Document document = editor.getDocument();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            InitialImplementation initialImplementation = getInitialImplementation(psiElement);

            TextRange textRange = psiElement.getTextRange();
            int startOffset = textRange.getStartOffset();
            int endOffset = textRange.getEndOffset();

            document.replaceString(startOffset, endOffset, initialImplementation.value);
            caret.moveToOffset(startOffset + initialImplementation.advanceOffset);
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        // Since I marked each property within a string, it acts as a reference point for knowing what is a property value.
        return editor != null && TypeAssistPsiUtil.isInTypeScriptObjectScope(psiElement)
                && psiElement.getNode().getElementType().equals(JSTokenTypes.STRING_LITERAL);
    }

    @Nls
    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    /**
     * Gets the initial implementation based ok current property value placeholder inserted by auto generated code.
     *
     * <p>If the value within the supplied {@code PsiElement} is 'string', the initial implementation is a pair
     * of string tokens according to the users STRING_STYLE setting, otherwise an empty string is returned which
     * removes the current value so the user can just start typing. The advanceOffset is just for nice UX.
     * For example, rather than position caret before the string, advancing it +1 puts caret within string.</p>
     *
     * @param element The {@code PsiElement} to create an initial implementation for.
     * @return {@code InitialImplementation}
     */
    private InitialImplementation getInitialImplementation(PsiElement element) {
        String value = StringUtil.stripQuotesAroundValue(element.getText());

        if (value.equals("string")) {
            String stringToken = TypeAssistApplicationSettings.getInstance().STRING_STYLE.getStyleToken();
            return new InitialImplementation(String.format("%s%s", stringToken, stringToken), 1);
        }
        return new InitialImplementation("");
    }

    @Override
    public Icon getIcon(int i) {
        return PluginIcons.TS_ASSIST;
    }

    private class InitialImplementation {
        private String value;
        private int advanceOffset;

        private InitialImplementation(String value, int advanceOffset) {
            this.value = value;
            this.advanceOffset = advanceOffset;
        }

        private InitialImplementation(String value) {
            this(value, 0);
        }
    }
}
