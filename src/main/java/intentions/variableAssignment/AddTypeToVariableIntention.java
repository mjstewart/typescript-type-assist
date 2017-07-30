package intentions.variableAssignment;

import com.intellij.codeInsight.intention.HighPriorityAction;
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction;
import com.intellij.lang.javascript.documentation.JSDocumentationProvider;
import com.intellij.lang.javascript.psi.JSVarStatement;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptVariable;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import icons.PluginIcons;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns out that this is not needed as jetbrains have already done an intention that assigns the explicit type to
 * a variable 'Specify type explicitly'. Keeping the code here for future reference.
 */
public class AddTypeToVariableIntention extends PsiElementBaseIntentionAction implements Iconable, HighPriorityAction {

    private final Pattern FUNCTION_PATTERN = Pattern.compile("<PRE>(\\[.*]) <b>");
    private final Pattern VARIABLE_PATTERN = Pattern.compile("<DD><code>(.*)</code></DD>");

    @Override
    public void invoke(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) throws IncorrectOperationException {
        if (editor == null) return;

        TypeScriptVariable typeScriptVariable = getTypeScriptVariable(psiElement);
        if (typeScriptVariable == null) return;
        JSVarStatement varStatement = PsiTreeUtil.getTopmostParentOfType(typeScriptVariable, JSVarStatement.class);
        if (varStatement == null) return;

        Caret caret = editor.getCaretModel().getCurrentCaret();
        Document document = editor.getDocument();
        TextRange range = varStatement.getTextRange();

        Consumer<String> writeCommand = value ->
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    document.deleteString(range.getStartOffset(), range.getEndOffset());
                    document.insertString(range.getStartOffset(), value);
                    caret.moveToOffset(range.getStartOffset() + value.length());
                    PsiDocumentManager.getInstance(project).commitDocument(document);
                });

        String doc = new JSDocumentationProvider().generateDoc(typeScriptVariable, typeScriptVariable.getOriginalElement());

        getType(doc).ifPresent(type -> {
            // Replace the variable name with the type information.
            String variableName = typeScriptVariable.getName();
            String replacement = variableName + ": " + type;
            String newStatement = varStatement.getText().replaceFirst("\\b" + variableName + "\\b", replacement);
            writeCommand.accept(newStatement);
        });
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, @NotNull PsiElement psiElement) {
        return editor != null && getTypeScriptVariable(psiElement) != null;
    }

    @NotNull
    @Override
    public String getText() {
        return "Add type to variable";
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

    public TypeScriptVariable getTypeScriptVariable(@NotNull PsiElement element) {
        PsiElement prevSibling = element.getPrevSibling();

        JSVarStatement varStatement;
        varStatement = PsiTreeUtil.getTopmostParentOfType(element, JSVarStatement.class);

        if (varStatement == null) {
            if (prevSibling instanceof JSVarStatement) {
                varStatement = (JSVarStatement) prevSibling;
            }
        }

        return (varStatement == null) ? null : PsiTreeUtil.findChildOfType(varStatement, TypeScriptVariable.class);
    }

    private Optional<String> getType(String docString) {
        if (docString.startsWith("<PRE>[")) {
            return getFunctionDocType(docString);
        }
        if (docString.startsWith("<DL><DT><b>Type:</b></DT><DD><code>")) {
            return getVariableDocType(docString);
        }
        return Optional.empty();
    }

    /**
     * Removes the outer brackets only if index 0 is an opening [ and there exists a close bracket ].
     *
     * <pre>
     *     [[]] => []
     * </pre>
     *
     * @param docType The current documentation value being processed.
     * @return The value with brackets removed otherwise an empty {@code Optional} if there were errors.
     */
    private Optional<String> removeOuterBrackets(String docType) {
        // Opening bracket if exists should be index 0, but substring ignores it so it gets set to 1.
        int openingBracketIndex = docType.indexOf("[") == 0 ? 1 : -1;
        int closeBracketIndex = docType.lastIndexOf("]");
        if (openingBracketIndex == -1 || closeBracketIndex == -1) return Optional.of(docType);
        return tryGet(() -> docType.substring(openingBracketIndex, closeBracketIndex).trim());
    }

    /**
     * Removes html tags that were escaped back into normal string form.
     *
     * @param docType The current documentation value being processed.
     * @return The formatted value otherwise an empty {@code Optional} if there were errors.
     */
    private Optional<String> removeHtmlTags(String docType) {
//        System.out.println("replaceEscapedCharacters: " + docType);
        return tryGet(() -> StringUtil.removeHtmlTags(docType).trim());
    }

    /**
     * When an expression is assigned to a variable, the documentation begins with the below string.
     * The pattern extracts the type info from within the code block.
     *
     * <pre>
     *     <DL><DT><b>Type:</b></DT><DD><code>(a: number) =&gt; (b: number) =&gt; (c: number) =&gt; number</code></DD></DL>
     * </pre>
     *
     * @param docString The complete documentation string created by the {@code JSDocumentationProvider}
     * @return The extracted type if it exists.
     */
    private Optional<String> getFunctionDocType(String docString) {
        Matcher matcher = FUNCTION_PATTERN.matcher(docString);
        if (matcher.find()) {
            return tryGet(() -> matcher.group(1))
                    .flatMap(this::removeOuterBrackets)
                    .flatMap(this::removeHtmlTags);
        }
        return Optional.empty();
    }

    /**
     * When an expression is assigned to a function, the documentation begins with the below string.
     * The pattern extracts the content within the first and last [ ] contained in the section before the function
     * name in bold <b>addMe</b>. The extracted value is processed further to get the formatted type.
     *
     * <p>------------- Example markup</p>
     *
     * <PRE>[ (b: number) =&gt; (c: number) =&gt; number ] <b>addMe</b>(&nbsp;[ number ] a&nbsp;)
     * </PRE>
     * <DL><DT><b>Parameters:</b></DT><DD><code>a</code></DD>
     * </DL>
     *
     * <p>------------- End example markup</p>
     *
     * @param docString The complete documentation string created by the {@code JSDocumentationProvider}
     * @return The extracted type if it exists.
     */
    private Optional<String> getVariableDocType(String docString) {
        Matcher matcher = VARIABLE_PATTERN.matcher(docString);
        if (matcher.find()) {
            return tryGet(() -> matcher.group(1))
                    .flatMap(this::removeHtmlTags);
        }
        return Optional.empty();
    }

    private <T> Optional<T> tryGet(Supplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
