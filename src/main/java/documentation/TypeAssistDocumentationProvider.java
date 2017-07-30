package documentation;

import codeInsight.TypeAssistPsiUtil;
import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.lang.javascript.documentation.JSDocumentationProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import documentation.objectProperties.TypeScriptObjectProperty;
import documentation.objectProperties.TypeScriptObjectPropertyGroup;
import documentation.textReplacement.*;
import documentation.types.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import settings.TypeAssistApplicationSettings;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/platform/lang-api/src/com/intellij/lang/documentation/DocumentationProvider.java
 *
 * <p>
 * Created by matt on 25-May-17.
 */
public class TypeAssistDocumentationProvider extends AbstractDocumentationProvider {
    public TypeAssistApplicationSettings settings = TypeAssistApplicationSettings.getInstance();

    /**
     * Types such as {@code null, undefined, any, void, never} are to receive syntax highlighting.
     * This list of {@code FindReplaceValue}s will be used during formatting to scan through the string and find/replace
     * any of these values with the new colored html span.
     *
     * @return The list of {@code FindReplaceValue}.
     */
    private List<FindReplaceValue> getUnspecifiedTypeReplacements() {
        List<FindReplaceValue> values = new ArrayList<>();
        values.add(FindReplaceValue.of("null",
                FindReplaceValue.wordBoundary("null"), HtmlUtils.span("null", settings.UNDEFINED_HEX_COLOR)));

        values.add(FindReplaceValue.of("undefined",
                FindReplaceValue.wordBoundary("undefined"), HtmlUtils.span("undefined", settings.UNDEFINED_HEX_COLOR)));

        values.add(FindReplaceValue.of("any",
                FindReplaceValue.wordBoundary("any"), HtmlUtils.span("any", settings.UNDEFINED_HEX_COLOR)));

        values.add(FindReplaceValue.of("void",
                FindReplaceValue.wordBoundary("void"), HtmlUtils.span("void", settings.UNDEFINED_HEX_COLOR)));

        values.add(FindReplaceValue.of("never",
                FindReplaceValue.wordBoundary("never"), HtmlUtils.span("never", settings.UNDEFINED_HEX_COLOR)));

        return values;
    }

    @Nullable
    @Override
    public String getQuickNavigateInfo(PsiElement element, PsiElement originalElement) {
        return super.getQuickNavigateInfo(element, originalElement);
    }

    @Override
    public List<String> getUrlFor(PsiElement element, PsiElement originalElement) {
        return super.getUrlFor(element, originalElement);
    }

    /**
     * Replaces all readonly keywords with the equivalent html span.
     *
     * @param value The value to format such as the full type getSignature.
     * @return The formatted String should readonly exist otherwise the supplied value without modification.
     */
    public String replaceReadOnly(String value) {
        return value.replaceAll(FindReplaceValue.wordBoundary("readonly"),
                HtmlUtils.span(HtmlUtils.code("readonly"), settings.READONLY_HEX_COLOR));
    }

    /**
     * Each {@code FindReplaceValue} in {@code this.getUnspecifiedTypeReplacements} is replaced in the supplied
     * {@code value} if it exists.
     *
     * @param value The value to format.
     * @return The formatted String with the new replacements if they exist.
     */
    public String replaceUnspecifiedTypes(String value) {
        for (FindReplaceValue replaceValue : getUnspecifiedTypeReplacements()) {
            value = value.replaceAll(replaceValue.getRegexSearchKey(), replaceValue.getReplacementValue());
        }
        return value;
    }

    /**
     * Replaces all optional properties with the equivalent html span.
     *
     * <p>The pattern matches all valid Optional types taking into consideration different spacing styles used
     * where there can be infinite white space between tokens and still match. While this makes no sense from
     * a code formatting point of view, it provides some flexibility into how the user styles their code where
     * extra white space between tokens is still acceptable.</p>
     *
     * <p>Additional flexibility is included for a variable name such as 'yes?ter?day' which is not treated as an Optional.
     * However if a question mark is placed as the last character its deemed Optional. Mapped optional types
     * are supported such as {@code [P in keyof Person]?}. Since it ends in a question mark its deemed as an Optional type.
     * Again, white space is catered for.</p>
     *
     * <pre>
     *     value = toString?(title?: string, firstName?: string, age?: number): string | undefined
     *     matchedOptionals = [title?, firstName?, age?]
     *
     *     Each matched optional gets transformed into a colored span.
     *     Each span is then replaced back into its corresponding original optional in the supplied value.
     * </pre>
     *
     * <p>String replacement is used as its simpler than recreating the complete type based on going through the
     * {@code PsiElement}.</p>
     *
     * @param value The value to format such as the full type getSignature.
     * @return The formatted String should any optionals exist otherwise the supplied value without modification.
     */
    public String replaceOptionals(String value) {
        // The pattern collects all valid optionals, plug it into regexpal and play around
        List<String> matchedOptionals = new ArrayList<>();
        String normalOptional = "([?\\w]+)?\\w+(\\s+)?\\?(?!(\\s+)?\\()(?![a-z])";
        String mappedType = "(\\s+)?\\[.*](\\s+)?\\?(\\s+)?";
        Matcher matcher = Pattern.compile(normalOptional + "|" + mappedType).matcher(value);
        while (matcher.find()) {
            // : is still included in the match but I want it removed so its not styled
            matchedOptionals.add(StringUtil.trimEnd(matcher.group(), ":"));
        }
        // Create a pair containing find and replacement values for the found optionalDeclaration.
        Function<String, Pair<String, String>> toOptionalReplacementTarget = optionalDeclaration ->
                Pair.create(StringUtil.escapeToRegexp(optionalDeclaration),
                        HtmlUtils.span(optionalDeclaration, settings.OPTIONAL_HEX_COLOR));

        // For each matched optional, map into a pair<lookup key, replacement span value> and issue the replacement
        // within the overall supplied value.
        return matchedOptionals.stream()
                .map(toOptionalReplacementTarget)
                .reduce(Pair.create(value, ""), (acc, element) ->
                        Pair.create(acc.first.replaceFirst(element.first, element.second), ""))
                .first;
    }

    /**
     * Creates a composed formatter consisting of all formatting transformations required to display the final
     * documentation.
     *
     * @return The composed formatter.
     */
    private Function<String, String> getFormatter() {
        Function<String, String> code = HtmlUtils::code;
        Function<String, String> readonly = this::replaceReadOnly;
        Function<String, String> optionals = this::replaceOptionals;
        Function<String, String> unspecifiedTypes = this::replaceUnspecifiedTypes;
        // Escape generics first so normal html isn't escaped after.
        Function<String, String> escape = StringUtil::escapeXml;

        if (settings.DOCUMENTATION_SYNTAX_HIGHLIGHTING) {
            // compose = read right to left. 'code' function runs last.
            return code
                    .compose(unspecifiedTypes)
                    .compose(readonly)
                    .compose(optionals)
                    .compose(escape);
        }
        // Basic formatting with no syntax coloring.
        return code.compose(escape);
    }

    /**
     * Transforms any generic type parameters for a given 'type name' such as Counter below into a colored span.
     *
     * <pre>
     *   interface Counter<Y> {
     *     ...
     *   }
     * </pre>
     *
     * <p>The {@code FindReplacePairs} contains all values which can be searched and replaced with this method.
     * For this example, Y would be the search key and the replacement value would be the colored span.</p>
     *
     * @param value            The type name such as {@code Counter<Y>}.
     * @param findReplacePairs The generic type parameter search/replacement values. There can be other values in here
     *                         such as resolvable references but its irrelevant as they all get replaced eventually.
     * @return The formatted structure type ready to be displayed in the documentation.
     */
    public String toTypeName(String value,
                             FindReplacePairs findReplacePairs) {
        for (FindReplaceValue replaceValue : findReplacePairs.getReplacementPairs()) {
            value = value.replaceAll(replaceValue.getRegexSearchKey(), replaceValue.getReplacementValue());
        }
        return value;
    }

    /**
     * Formats the {@code DescribableType} into a displayable documentation String by applying the formatter. The
     * {@code FindReplacePair} argument contains search key/replacement value pairs that get searched and replaced
     * in the formattedType. These include replacing resolvable types with hyperlinks and generic parameters with
     * a colored span.
     * <p>
     * <p>This method is only called for types where it makes sense to apply the formatting.
     * For example, type alias literals do not need formatting.</p>
     *
     * @param describableType  The {@code DescribableType} to create documentation for.
     * @param findReplacePairs Contains key/replacement value pairs.
     * @return The formatted documentation String ready for displaying.
     */
    private String toDocumentationType(DescribableType describableType,
                                       FindReplacePairs findReplacePairs) {
        String formattedType = getFormatter().apply(describableType.getType()) + HtmlUtils.newLine();
        for (FindReplaceValue replaceValue : findReplacePairs.getReplacementPairs()) {
            formattedType = formattedType.replaceAll(replaceValue.getRegexSearchKey(), replaceValue.getReplacementValue());
        }
        return formattedType;
    }

    /**
     * Callback for asking the doc provider for the complete documentation.
     * <p>
     * <p>Underlying implementation may be time-consuming, that's why this method is expected not to be called from EDT.</p>
     *
     * @param element         the element for which the documentation is requested (for example, if the mouse is over
     *                        a method reference, this will be the method to which the reference is resolved).
     * @param originalElement the element under the mouse cursor
     * @return target element's documentation, or {@code null} if provider is unable to generate documentation
     * for the given element
     */
    @Override
    public String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
        if (element == null) {
            return "No documentation available";
        }
        StringBuilder docBuilder = new StringBuilder();

        TypeDescription typeDescription = TypeDescription.create(element);
        if (typeDescription.isValid()) {
            // Always substitute in hyperlinks for resolvable types regardless of whether syntax highlighting is on.
            FindReplaceResolvableReference findReplaceResolvableReference =
                    FindReplaceResolvableReference.of(TypeAssistPsiUtil.collectResolvableReferences(element), typeDescription.getTypeName());

            List<String> genericTypeParameters = new ArrayList<>();
            if (settings.DOCUMENTATION_SYNTAX_HIGHLIGHTING) {
                genericTypeParameters = TypeAssistPsiUtil.collectGenericTypeParameters(element);
            }
            FindReplaceGenericTypeParameter findReplaceGenericTypeParameter =
                    FindReplaceGenericTypeParameter.of(genericTypeParameters);

            FindReplacePairs findReplacePairs =
                    FindReplacePairs.of(findReplaceResolvableReference, findReplaceGenericTypeParameter);

            String typeName = toTypeName(getFormatter().apply(typeDescription.getTypeName()), findReplacePairs);
            docBuilder.append(typeName).append(": ");
            docBuilder.append(HtmlUtils.bold(typeDescription.getTypeDefinition().getDescription()));

            switch (typeDescription.getTypeDefinition()) {
                case Interface:
                case TypeAliasObject:
                    docBuilder.append(HtmlUtils.horizontalLine());
                    TypeScriptObjectProperty.of(element)
                            .ifPresent(propertyGroup -> writeObjectDocumentation(propertyGroup, docBuilder, findReplacePairs));
                    break;
                case TypeAliasSingle:
                    SingleType.of(element)
                            .ifPresent(singleTypeValue -> docBuilder.append(HtmlUtils.horizontalLine())
                                    .append(toDocumentationType(singleTypeValue, findReplacePairs)));
                    break;
                case TypeAliasLiteral:
                    LiteralType.of(element)
                            .ifPresent(literalType -> docBuilder.append(HtmlUtils.newLine())
                                    .append(HtmlUtils.code(literalType.getType())).append(HtmlUtils.newLine()));
                    break;
                case TypeAliasFunction:
                    FunctionType.of(element)
                            .ifPresent(functionType -> docBuilder.append(HtmlUtils.newLine())
                                    .append(toDocumentationType(functionType, findReplacePairs))
                                    .append(HtmlUtils.newLine()));
                    break;
                case TypeAliasMappedType:
                    MappedType.of(element)
                            .ifPresent(mappedTypeValue -> docBuilder.append(HtmlUtils.newLine())
                                    .append(toDocumentationType(mappedTypeValue, findReplacePairs)));
                    break;
                case Union:
                case Intersection:
                    UnionOrIntersectionType.of(element)
                            .ifPresent(unionOrIntersectionValueList -> {
                                docBuilder.append(" (").append(unionOrIntersectionValueList.size()).append(")").append(HtmlUtils.horizontalLine());
                                unionOrIntersectionValueList.stream()
                                        .map(unionOrIntersectionValue -> toDocumentationType(unionOrIntersectionValue, findReplacePairs))
                                        .forEach(docBuilder::append);
                            });
                    break;
                case Enum:
                    EnumField.of(element)
                            .ifPresent(enumValueList -> {
                                docBuilder.append(" (").append(enumValueList.size()).append(")").append(HtmlUtils.horizontalLine());
                                enumValueList.stream()
                                        .map(enumValue -> toDocumentationType(enumValue, findReplacePairs))
                                        .forEach(docBuilder::append);
                            });
                    break;
                case None:
                    docBuilder.append("No documentation available").append(HtmlUtils.newLine());
            }
        }

        // Standard docs are printed below type information regardless of whether type info exists.
        String standardDocs = new JSDocumentationProvider().generateDoc(element, originalElement);

        if (standardDocs != null) {
            docBuilder.append((typeDescription.isValid() ? HtmlUtils.newLine() : ""))
                    .append(HtmlUtils.bold("Standard Documentation"))
                    .append(HtmlUtils.horizontalLine())
                    .append(standardDocs);
        }

        String documentation = docBuilder.toString();
        return documentation.isEmpty() ? null : documentation;
    }

    @Override
    public PsiElement getDocumentationElementForLookupItem(PsiManager psiManager, Object object, PsiElement element) {
        return super.getDocumentationElementForLookupItem(psiManager, object, element);
    }

    @Override
    public PsiElement getDocumentationElementForLink(PsiManager psiManager, String link, PsiElement context) {
        return super.getDocumentationElementForLink(psiManager, link, context);
    }

    @Nullable
    @Override
    public PsiElement getCustomDocumentationElement(@NotNull Editor editor, @NotNull PsiFile file, @Nullable PsiElement contextElement) {
        return super.getCustomDocumentationElement(editor, file, contextElement);
    }

    @Nullable
    @Override
    public Image getLocalImageForElement(@NotNull PsiElement element, @NotNull String imageSpec) {
        return super.getLocalImageForElement(element, imageSpec);
    }

    /**
     * Writes documentation for all {@code TypeScriptObjectPropertyGroup} categories into the supplied
     * {@code StringBuilder}.
     *
     * @param propertyGroup    Contains the categories.
     * @param docBuilder       {@code StringBuilder} to write the generated documentation to.
     * @param findReplacePairs Contains key/replacement value pairs.
     */
    private void writeObjectDocumentation(TypeScriptObjectPropertyGroup propertyGroup,
                                          StringBuilder docBuilder,
                                          FindReplacePairs findReplacePairs) {
        if (!propertyGroup.getPropertySignatures().isEmpty()) {
            String title = "Properties (" + propertyGroup.getPropertySignatures().size() + ")";
            docBuilder.append(HtmlUtils.heading(title));

            standardWriteObjectDocumentation(propertyGroup.getPropertySignatures(), findReplacePairs, docBuilder);
            docBuilder.append(HtmlUtils.newLine());
        }

        if (!propertyGroup.getIndexableProperties().isEmpty()) {
            String title = "Indexable (" + propertyGroup.getIndexableProperties().size() + ")";
            docBuilder.append(HtmlUtils.heading(title));

            standardWriteObjectDocumentation(propertyGroup.getIndexableProperties(), findReplacePairs, docBuilder);
            docBuilder.append(HtmlUtils.newLine());
        }

        if (!propertyGroup.getHybridFunctionProperties().isEmpty()) {
            String title = "Hybrid Functions (" + propertyGroup.getHybridFunctionProperties().size() + ")";
            docBuilder.append(HtmlUtils.heading(title));

            standardWriteObjectDocumentation(propertyGroup.getHybridFunctionProperties(), findReplacePairs, docBuilder);
            docBuilder.append(HtmlUtils.newLine());
        }

        if (!propertyGroup.getFunctionProperties().isEmpty()) {
            String title = "Functions (" + propertyGroup.getFunctionProperties().size() + ")";
            docBuilder.append(HtmlUtils.heading(title));

            standardWriteObjectDocumentation(propertyGroup.getFunctionProperties(), findReplacePairs, docBuilder);
            docBuilder.append(HtmlUtils.newLine());
        }
    }

    private void standardWriteObjectDocumentation(List<TypeScriptObjectProperty> properties,
                                                  FindReplacePairs findReplacePairs,
                                                  StringBuilder docBuilder) {
        writeObjectDocumentation(properties,
                (objectProperty -> toDocumentationType(objectProperty, findReplacePairs)),
                docBuilder::append);
    }

    private void writeObjectDocumentation(List<TypeScriptObjectProperty> properties,
                                          Function<TypeScriptObjectProperty, String> mapper,
                                          Consumer<String> finisher) {
        properties.stream().map(mapper).forEach(finisher);
    }
}
