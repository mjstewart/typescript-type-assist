package actions;

import codeInsight.TypeAssistPsiUtil;
import codeInsight.codeGeneration.AssignableArrayCreator;
import codeInsight.codeGeneration.AssignableObjectCreator;
import codeInsight.codeGeneration.PropertyArrayCreator;
import codeInsight.codeGeneration.PropertyObjectCreator;
import codeInsight.instructions.ArrayInsertInstruction;
import codeInsight.instructions.InsertInstruction;
import com.intellij.lang.ASTNode;
import com.intellij.lang.javascript.JSTokenTypes;
import com.intellij.lang.javascript.psi.*;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptObjectType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.HighlighterTargetArea;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import documentation.objectProperties.TypeScriptObjectProperty;
import documentation.objectProperties.TypeScriptObjectPropertyGroup;
import documentation.types.TypeDescription;
import highligher.PropertyValueHighlightManager;
import settings.TypeAssistApplicationSettings;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Main entry point for auto generating code based on the triggering context as described in {@link CreateContext}.
 * <p>
 * Created by matt on 26-May-17.
 */
public class CreateTypeScriptObjectAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        PsiFile file = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        Caret caret = anActionEvent.getData(CommonDataKeys.CARET);
        PsiElement psiElement = anActionEvent.getData(CommonDataKeys.PSI_ELEMENT);

        if (project == null || file == null || editor == null || caret == null || psiElement == null) {
            return;
        }

        Document document = editor.getDocument();
        TypeDescription typeDescription = TypeDescription.create(psiElement);

        if (!typeDescription.isValid()) return;
        Optional<TypeScriptObjectPropertyGroup> optionalPropertyInfo = TypeScriptObjectProperty.of(psiElement);
        if (!optionalPropertyInfo.isPresent()) return;
        List<TypeScriptObjectProperty> objectProperties = optionalPropertyInfo.get().getPropertySignatures();

        // Identifies which type of code to generate and where to insert it.
        InsertInstruction insertInstruction = getInsertContext(anActionEvent);
        if (!insertInstruction.isValid()) return;

        // Get project level plugin settings.
        TypeAssistApplicationSettings typeAssistApplicationSettings = TypeAssistApplicationSettings.getInstance();

        WriteCommandAction.runWriteCommandAction(project, () -> {
            // Delete current line since it will be replaced with the new generated code.
            int offset = insertInstruction.getOffset();
            document.deleteString(offset, caret.getVisualLineEnd());
            document.insertString(offset, generateCode(objectProperties, insertInstruction, typeDescription, typeAssistApplicationSettings));

            // The document must be commit before running the formatter.
            PsiDocumentManager.getInstance(project).commitDocument(document);

            formatAndHighlight(insertInstruction, anActionEvent, typeAssistApplicationSettings);
        });
    }

    @Override
    public void update(AnActionEvent e) {
        e.getPresentation().setEnabled(getInsertContext(e).getCreateContext() != CreateContext.None);
    }

    /**
     * Generates code containing all properties within the {@code TypeScriptObjectType} and formatted according to
     * {@code TypeAssistApplicationSettings}.
     *
     * @param objectProperties              The {@code List<TypeScriptObjectProperty>} containing the properties to generate.
     * @param insertInstruction             The {@code InsertInstruction} which contains context and best insertion point.
     * @param typeDescription               The {@code TypeDescription}.
     * @param typeAssistApplicationSettings The {@code TypeAssistApplicationSettings}.
     * @return The generated code as a {@code String}.
     */
    private String generateCode(List<TypeScriptObjectProperty> objectProperties,
                                InsertInstruction insertInstruction,
                                TypeDescription typeDescription,
                                TypeAssistApplicationSettings typeAssistApplicationSettings) {
        switch (insertInstruction.getCreateContext()) {
            case AssignableObject:
                return new AssignableObjectCreator(objectProperties, typeDescription, typeAssistApplicationSettings).generate();
            case PropertyObject:
                return new PropertyObjectCreator(objectProperties, insertInstruction, typeAssistApplicationSettings).generate();
            case AssignableArray:
                return new AssignableArrayCreator(objectProperties,
                        (ArrayInsertInstruction) insertInstruction, typeDescription, typeAssistApplicationSettings).generate();
            case PropertyArray:
                return new PropertyArrayCreator(objectProperties,
                        (ArrayInsertInstruction) insertInstruction, typeAssistApplicationSettings).generate();
        }

        Notifications.Bus.notify(new Notification(
                "CreateTypeScriptObjectAction",
                "Code Generation",
                "Code generation failed",
                NotificationType.ERROR));
        return "";
    }

    /**
     * Formats the generated code according to the users code style settings in addition to those provided by
     * {@code TypeAssistApplicationSettings}.
     *
     * @param insertInstruction             The {@code InsertInstruction} which contains context and best insertion point.
     * @param anActionEvent                 The {@code AnActionEvent} is supplied rather than pass in a million parameters.
     * @param typeAssistApplicationSettings The {@code TypeAssistApplicationSettings}.
     */
    private void formatAndHighlight(InsertInstruction insertInstruction,
                                    AnActionEvent anActionEvent,
                                    TypeAssistApplicationSettings typeAssistApplicationSettings) {
        Project project = anActionEvent.getData(CommonDataKeys.PROJECT);
        PsiFile file = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);

        if (project == null || file == null || editor == null) {
            return;
        }

        // Refers to the variable declaration
        JSVarStatement varStatement = PsiTreeUtil.findElementOfClassAtOffset(file, insertInstruction.getOffset(),
                JSVarStatement.class, false);

        if (varStatement != null) {
            // reformatNewlyAddedElement works the best, a little hit and miss just using reformat.
            // Gives access to triggering reformatting based on users style settings
            CodeStyleManager.getInstance(project)
                    .reformatNewlyAddedElement(varStatement.getParent().getNode(), varStatement.getNode());
        }

        if (!typeAssistApplicationSettings.PROPERTY_HIGHLIGHTING) return;

        // Perform highlighting based on settings.
        Optional<HighlightInstruction> highlightingInfo = getHighlightInstruction(insertInstruction, varStatement, file);
        if (!highlightingInfo.isPresent()) return;
        HighlightInstruction highlightInstruction = highlightingInfo.get();

        TextAttributes textAttributes = new TextAttributes();
        textAttributes.setEffectType(typeAssistApplicationSettings.PROPERTY_HIGHLIGHT_STYLE.getEffectType());
        textAttributes.setEffectColor(typeAssistApplicationSettings.toColor(typeAssistApplicationSettings.PROPERTY_HIGHLIGHT_HEX_COLOR));

        PropertyValueHighlightManager propertyValueHighlightManager = new PropertyValueHighlightManager();
        Collection<JSLiteralExpression> pendingPropertyValues =
                PsiTreeUtil.findChildrenOfType(highlightInstruction.rootJsLiteralEntryPoint, JSLiteralExpression.class);

        for (JSLiteralExpression pendingPropertyValue : pendingPropertyValues) {
            TextRange textRange = pendingPropertyValue.getTextRange();

            RangeHighlighter rangeHighlighter = editor.getMarkupModel()
                    .addRangeHighlighter(textRange.getStartOffset(), textRange.getEndOffset(),
                            HighlighterLayer.SELECTION, textAttributes, HighlighterTargetArea.EXACT_RANGE);

            propertyValueHighlightManager.add(rangeHighlighter);
        }

        // Move caret to first property needing implementing for nice UX.
        editor.getCaretModel().getCurrentCaret().moveToOffset(highlightInstruction.bestCaretOffsetForNextAction);

        Document document = editor.getDocument();
        document.addDocumentListener(new PropertyValueHighlightDocumentListener(editor, document, propertyValueHighlightManager));

    }

    private Optional<HighlightInstruction> getHighlightInstruction(InsertInstruction insertInstruction, JSVarStatement varStatement, PsiFile file) {
        switch (insertInstruction.getCreateContext()) {
            case AssignableObject:
            case AssignableArray:
                /*
                 * The supplied JSVarStatement refers to the object root. It will collect ALL properties to highlight.
                 * The only thing to do is to set the caret to the first property value to implement.
                 */
                JSLiteralExpression firstPropertyValue = PsiTreeUtil.findChildOfType(varStatement, JSLiteralExpression.class);
                if (firstPropertyValue != null) {
                    return Optional.of(new HighlightInstruction(varStatement, firstPropertyValue.getTextOffset() + 1));
                }
            case PropertyObject:
            case PropertyArray:
                /*
                 * The idea here is that you don't want to start at the top most variable declaration, otherwise all
                 * existing properties will be re highlighted. Use the new object literal expression
                 * and base that as the root to collect sub properties of the new object.
                 */
                PsiElement elementAtPropertyInsert = file.findElementAt(insertInstruction.getOffset());
                PsiElement rootElement;

                // See if we are in a PropertyArray context first.
                rootElement = PsiTreeUtil.getParentOfType(elementAtPropertyInsert, JSArrayLiteralExpression.class);

                if (rootElement == null) {
                    // Must be a normal PropertyObject
                    rootElement = PsiTreeUtil.getParentOfType(elementAtPropertyInsert, JSObjectLiteralExpression.class);
                }

                // First child corresponds to the first property to position caret nicely.
                JSLiteralExpression firstSubPropertyValue = PsiTreeUtil.findChildOfType(rootElement, JSLiteralExpression.class);
                if (firstSubPropertyValue != null) {
                    return Optional.of(new HighlightInstruction(rootElement, firstSubPropertyValue.getTextOffset() + 1));
                }
            default:
                return Optional.empty();
        }
    }


    /**
     * Based on where the action was triggered determines how to create the new object, since code generation may be
     * different for each context.
     *
     * <p>For example, a standalone object needs to be assigned to a variable with a const keyword and variable name.
     * However, an object created from a property type needs no assignment.</p>
     *
     * <p>See <a href="http://www.jetbrains.org/intellij/sdk/docs/basics/architectural_overview/psi_elements.html">
     * http://www.jetbrains.org/intellij/sdk/docs/basics/architectural_overview/psi_elements.html</a>.</p>
     *
     * <p>Its vital to understand which PsiElement is being referred to when triggered.
     * When the action is triggered, the PsiElement is resolved to the TypeScript type presumably located in
     * some TypeScript types file. To get the unresolved element sitting where the caret is in the current editor you
     * need to use PsiFile.findElementAt. Using the unresolved element is needed so the surrounding code can
     * be analysed to determine the context such as if its an array type or is it within a function etc.</p>
     *
     * @return {@code InsertInstruction}.
     */
    private InsertInstruction getInsertContext(AnActionEvent anActionEvent) {
        PsiFile file = anActionEvent.getData(CommonDataKeys.PSI_FILE);
        Editor editor = anActionEvent.getData(CommonDataKeys.EDITOR);
        Caret caret = anActionEvent.getData(CommonDataKeys.CARET);
        PsiElement resolvedPsiElement = anActionEvent.getData(CommonDataKeys.PSI_ELEMENT);

        if (editor == null || file == null || caret == null || resolvedPsiElement == null) {
            return InsertInstruction.none();
        }

        Optional<TypeScriptObjectType> typeScriptObjectOptional = TypeAssistPsiUtil.getChildTypeScriptObject(resolvedPsiElement);
        // The resolved PsiElement that triggered the action is not a TypeScriptObject.
        if (!typeScriptObjectOptional.isPresent()) return InsertInstruction.none();

        // Now get the actual element triggered in this files source code to find best insertion offset.
        PsiElement unresolvedElement = file.findElementAt(caret.getOffset());
        return findBestInsertOffset(CreateContext.AssignableObject, unresolvedElement)
                .orElseTry(() -> findBestInsertOffset(CreateContext.PropertyObject, unresolvedElement));
    }

    /**
     * Convenience method to call the concrete method responsible for finding the best insert offset for a given
     * {@code CreateContext}.
     *
     * <p>Decided to base logic on finding the best insert position since it reduces duplicate code.
     * Whenever a {@code CreateContext} is needed, if this function returns {@code ILLEGAL_INSERT_OFFSET}, you know
     * that the supplied context is invalid and you can try another one.
     * This type of logic is seen in {@code getInsertContext}</p>
     *
     * <p>Motivation: {@code update} can get by with only knowing the {@code CreateContext}, but map {@code actionPerformed}
     * needs to know both the {@code CreateContext} and the best insert offset for inserting the new code.
     * The logic to determine the {@code CreateContext} is identical so to avoid creating 2 methods</p>
     *
     * @param element The unresolved PsiElement referring to the element in the current file.
     *                See javadoc for {@link #getInsertContext}
     * @return {@code InsertInstruction}
     */
    private InsertInstruction findBestInsertOffset(CreateContext createContext, PsiElement element) {
        switch (createContext) {
            case AssignableObject:
                return findBestAssignableObjectInsertOffset(element)
                        .map(instruction -> getArrayInsertInstruction(instruction, element));
            case PropertyObject:
                return findBestPropertyInsertOffset(element)
                        .map(instruction -> getArrayInsertInstruction(instruction, element));
            default:
                return InsertInstruction.none();
        }
    }

    /**
     * Within the context of a property field, a valid insertion offset will be calculated only if there is a colon.
     * The colon is used as a reference point to insert the new object - see examples below.
     * <p>
     * <pre>
     *     street: string     - valid
     *     postcode number    - invalid
     * </pre>
     *
     * @param element The unresolved PsiElement referring to the element in the current file.
     *                See javadoc for {@link #getInsertContext}
     * @return {@code InsertInstruction}
     */
    private InsertInstruction findBestPropertyInsertOffset(PsiElement element) {
        // Activate create object action when caret within or 1 after PsiElement.
        JSProperty jsProperty = PsiTreeUtil.getTopmostParentOfType(element, JSProperty.class);
        JSProperty jsPrevSibling = PsiTreeUtil.getPrevSiblingOfType(element, JSProperty.class);
        JSProperty entryProperty = jsProperty == null ? jsPrevSibling : jsProperty;

        if (entryProperty == null) {
            return InsertInstruction.none();
        }

        ASTNode[] children = entryProperty.getNode().getChildren(TokenSet.create(JSTokenTypes.COLON));
        if (children.length == 1) {
            // Advance 2 spaces ahead of the colon ready for the next insert point, eg person:<2 spaces>{ ... }
            int offset = children[0].getPsi().getTextOffset() + 2;
            boolean isLastProperty = PsiTreeUtil.getNextSiblingOfType(entryProperty, JSProperty.class) == null;
            return InsertInstruction.of(CreateContext.PropertyObject, offset, isLastProperty);
        }
        return InsertInstruction.none();
    }

    /**
     * Find best insert offset for an object that can be assigned to a variable.
     *
     * @param element The unresolved PsiElement referring to the element in the current file.
     *                See javadoc for {@link #getInsertContext}
     * @return {@code InsertInstruction}
     */
    private InsertInstruction findBestAssignableObjectInsertOffset(PsiElement element) {
        // Handle case: Activate when caret within Type.
        JSExpressionStatement jsExpressionParent = PsiTreeUtil.getTopmostParentOfType(element, JSExpressionStatement.class);
        // Handle case: Activate on 1 whitespace after Type.
        JSExpressionStatement jsExpressionPrevSibling = PsiTreeUtil.getPrevSiblingOfType(element, JSExpressionStatement.class);
        JSExpressionStatement jsExpression = jsExpressionParent == null ? jsExpressionPrevSibling : jsExpressionParent;

        if (jsExpression == null) {
            return InsertInstruction.none();
        }

        return InsertInstruction.of(CreateContext.AssignableObject, jsExpression.getTextOffset());
    }

    /**
     * The {@code existingInstruction} contains the base context such as if its an assignable or property based object.
     *
     * <p>This method further analyses whether its array based denoted by trailing []. Optionally a value can be
     * provided which determines the size of the array to create as explained below</p>
     *
     * <pre>
     *     Person[]  -> Size 1
     *     Person[4] -> Size 4
     *     Person[blah] -> Non numeric long value so provide a default of 1.
     * </pre>
     *
     * <p>If its not array based, the {@code existingInstruction} is returned unchanged to promote chaining calls.</p>
     *
     * @param element The unresolved PsiElement referring to the element in the current file.
     *                See javadoc for {@link #getInsertContext}
     * @return {@code InsertInstruction}
     */
    private InsertInstruction getArrayInsertInstruction(InsertInstruction existingInstruction, PsiElement element) {
        JSIndexedPropertyAccessExpression indexedProperty = PsiTreeUtil.getTopmostParentOfType(element, JSIndexedPropertyAccessExpression.class);

        // Not an array so just return existing instruction so further chaining can be done if need be.
        if (indexedProperty == null) return existingInstruction;

        // Upgrades base context to an array depending on if its standalone or within an existing property.
        CreateContext arrayContext;
        switch (existingInstruction.getCreateContext()) {
            case AssignableObject:
                arrayContext = CreateContext.AssignableArray;
                break;
            case PropertyObject:
                arrayContext = CreateContext.PropertyArray;
                break;
            default:
                arrayContext = existingInstruction.getCreateContext();
        }

        /*
         * 2 scenarios
         *
         * 1. When using a type that is imported, Person[3] only has 1 JSLiteral containing the number.
         *
         * 2. When using a type that is defined in the same file (not imported), the PsiElement is not resolved
         *    within an array such as 'Person[3]'. The syntax to get the resolution to happen needs to be 'Person'[3]
         *    with [3] outside the string. There are now 2 JSLiterals with the second literal referring
         *    to size 3 rather than the first literal as in the first example.
         */
        JSLiteralExpression[] jsLiterals = PsiTreeUtil.getChildrenOfType(indexedProperty, JSLiteralExpression.class);

        // Determine if this is the last property in the object.
        JSProperty thisProperty = PsiTreeUtil.getParentOfType(indexedProperty, JSProperty.class);
        boolean isLastProperty = PsiTreeUtil.getNextSiblingOfType(thisProperty, JSProperty.class) == null;

        if (jsLiterals == null) {
            // Person[] implies size 1.
            return ArrayInsertInstruction.of(arrayContext, existingInstruction.getOffset(), isLastProperty, 1);
        } else {
            // Handles scenario 1 and 2 by going through all literals and finding the numeric value. -1 if no number found.
            long arrayCreationSize = -1;
            for (JSLiteralExpression jsLiteral : jsLiterals) {
                if (jsLiteral.getValue() instanceof Long) {
                    arrayCreationSize = (long) jsLiteral.getValue();
                }
            }

            if (arrayCreationSize == -1) {
                // Provide sensible default for garbage value within ['blah'].
                return ArrayInsertInstruction.of(arrayContext, existingInstruction.getOffset(), isLastProperty, 1);
            }
            // Person[4] implies create an array of size 4.
            return ArrayInsertInstruction.of(arrayContext, existingInstruction.getOffset(), isLastProperty, arrayCreationSize);
        }
    }

    private static class PropertyValueHighlightDocumentListener implements DocumentListener {

        private Editor editor;
        private Document document;
        private PropertyValueHighlightManager propertyValueHighlightManager;

        public PropertyValueHighlightDocumentListener(Editor editor, Document document, PropertyValueHighlightManager propertyValueHighlightManager) {
            this.editor = editor;
            this.document = document;
            this.propertyValueHighlightManager = propertyValueHighlightManager;
        }

        @Override
        public void beforeDocumentChange(DocumentEvent documentEvent) {
            if (propertyValueHighlightManager.isAllowedToRun(documentEvent.getOffset())) {
                propertyValueHighlightManager.get(documentEvent.getOffset())
                        .ifPresent(rangeHighlighter -> {
                            editor.getMarkupModel().removeHighlighter(rangeHighlighter);
                            propertyValueHighlightManager.remove(rangeHighlighter);
                        });

                if (propertyValueHighlightManager.isEmpty()) {
                    document.removeDocumentListener(this);
                }
            }
        }

        @Override
        public void documentChanged(DocumentEvent documentEvent) { }
    }

    private static class HighlightInstruction {
        private PsiElement rootJsLiteralEntryPoint;
        private int bestCaretOffsetForNextAction;

        private HighlightInstruction(PsiElement rootJsLiteralEntryPoint, int bestCaretOffsetForNextAction) {
            this.rootJsLiteralEntryPoint = rootJsLiteralEntryPoint;
            this.bestCaretOffsetForNextAction = bestCaretOffsetForNextAction;
        }
    }
}
