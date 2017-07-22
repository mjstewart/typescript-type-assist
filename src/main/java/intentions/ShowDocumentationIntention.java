package intentions;

import codeInsight.TypeAssistPsiUtil;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiElement;
import com.intellij.util.IncorrectOperationException;
import documentation.types.TypeDescription;
import icons.PluginIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * Creates an intention to open the javadoc as per the implementation by {@code TypeAssistDocumentationProvider}.
 * This intention exists purely for convenience as its sometimes useful to trigger the javadoc this way as opposed to
 * hovering the mouse over the type.
 *
 * Created by matt on 26-May-17.
 */
public class ShowDocumentationIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        TypeDescription typeDescription = TypeAssistPsiUtil.getResolvedTypeDescription(psiElement);
        if (!typeDescription.isValid()) return;
        if (DocumentationManager.getInstance(project) == null) return;
        DocumentationManager.getInstance(project).showJavaDocInfo(typeDescription.getResolvedElement(), psiElement);
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        return editor != null && TypeAssistPsiUtil.getResolvedTypeDescription(psiElement).isValid();
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
        return "Show typescript docs";
    }


    @Override
    public Icon getIcon(int i) {
        return PluginIcons.TS_ASSIST;
    }
}
