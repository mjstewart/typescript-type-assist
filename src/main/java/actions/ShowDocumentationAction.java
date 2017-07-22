package actions;

import codeInsight.TypeAssistPsiUtil;
import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import documentation.types.TypeDescription;

/**
 * Action that opens the java doc popup showing the typescript documentation.
 * <p>
 * Created by matt on 24-May-17.
 */
public class ShowDocumentationAction extends AnAction {
    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        PsiFile file = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        PsiElement psiElement = anActionEvent.getData(CommonDataKeys.PSI_ELEMENT);

        if (editor == null || file == null || project == null || psiElement == null) {
            anActionEvent.getPresentation().setEnabled(false);
            return;
        }

        TypeDescription typeDescription = TypeDescription.create(psiElement);
        if (!typeDescription.isValid()) return;
        if (DocumentationManager.getInstance(project) == null) return;

        // Note that in AnAction, psiElement is already resolved to typescript type unlike an intention so it is
        // not necessary to use the resolved type within TypeDescription as they refer to the same element.
        DocumentationManager.getInstance(project).showJavaDocInfo(psiElement, psiElement.getOriginalElement());
    }

    @Override
    public void update(AnActionEvent e) {
        Editor editor = e.getData(CommonDataKeys.EDITOR);
        PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
        PsiElement psiElement = e.getData(CommonDataKeys.PSI_ELEMENT);

        if (editor == null || file == null || psiElement == null) {
            e.getPresentation().setEnabled(false);
            return;
        }
        e.getPresentation().setEnabled(TypeAssistPsiUtil.isTypeOfInterest(psiElement));
    }
}
