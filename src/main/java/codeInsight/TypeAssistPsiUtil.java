package codeInsight;

import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.ecma6.*;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiQualifiedReference;
import com.intellij.psi.util.PsiTreeUtil;
import documentation.types.TypeDescription;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Utilities similar to {@code PsiTreeUtil} but for typescript.
 *
 * Created by matt on 03-Jun-17.
 */
public class TypeAssistPsiUtil {

    /**
     * Used for {@code ShowDocumentationIntention} when the {@code PsiElement} is not resolved. Since its an intention, there
     * is no {@code AnActionEvent} supplied with the resolved {@code PsiElement}, hence we need to do it manually. If the
     * supplied {@code PsiElement} is resolvable, its checked to see if it satisfies {@link #isTypeOfInterest} and
     * if so the corresponding {@code TypeDescription} is returned.
     *
     * @param psiElement The {@code PsiElement} to resolve.
     * @return {@code TypeDescription}
     */
    public static TypeDescription getResolvedTypeDescription(@NotNull PsiElement psiElement) {
        // Handle case where caret is within the Type name.
        Optional<PsiElement> resolvedWithinCaret = Optional.ofNullable(psiElement.getParent())
                .flatMap(parent -> Optional.of(parent.getReferences()))
                .flatMap(references -> {
                    if (references.length > 0) {
                        return Optional.ofNullable(references[0].resolve());
                    }
                    return Optional.empty();
                });

        if (resolvedWithinCaret.isPresent() && isTypeOfInterest(resolvedWithinCaret.get())) {
            return TypeDescription.create(resolvedWithinCaret.get());
        }

        // Handle case where caret is trailing the Type name by 1 whitespace.
        Optional<PsiElement> resolvedTrailingCaret = Optional.ofNullable(psiElement.getPrevSibling())
                .flatMap(jsExpressionStatement -> Optional.ofNullable(jsExpressionStatement.getFirstChild()))
                .flatMap(firstChild -> Optional.of(firstChild.getReferences()))
                .flatMap(references -> {
                    if (references.length > 0) {
                        return Optional.ofNullable(references[0].resolve());
                    }
                    return Optional.empty();
                });

        if (resolvedTrailingCaret.isPresent() && isTypeOfInterest(resolvedTrailingCaret.get())) {
            return TypeDescription.create(resolvedTrailingCaret.get());
        }
        return TypeDescription.none();
    }

    /**
     * Determines if the supplied element is an instance of any of the below (eligible for documentation display)
     * <pre>TypeScriptInterface, TypeScriptTypeAlias, TypeScriptEnum</pre>
     *
     * <p>Pay careful attention to the {@code PsiElement} supplied. It must be fully resolved. For example, a
     * {@code PsiElement} within an {@code AnActionEvent} is already resolved to the target. If its not resolved
     * then {@link #getResolvedTypeDescription} may be helpful.</p>
     *
     * @param element The resolved element to check.
     * @return {@code true} if the element is an instance of the mentioned types.
     */
    public static boolean isTypeOfInterest(@NotNull PsiElement element) {
        return element instanceof TypeScriptInterface
                || element instanceof TypeScriptTypeAlias
                || element instanceof TypeScriptEnum;
    }

    /**
     * From the supplied {@code PsiElement}, try locate a {@code TypeScriptObjectType} as a child.
     *
     * <p>The Optional only contains a valid value if a {@code TypeScriptObjectType} is found contains at least
     * 1 property.</p>
     *
     * @param psiElement The {@code PsiElement} to locate a child {@code TypeScriptObjectType} from.
     * @return Optional {@code TypeScriptObjectType}
     */
    public static Optional<TypeScriptObjectType> getChildTypeScriptObject(@NotNull PsiElement psiElement) {
        // Handles edge case where there is an object in parameter list such as 'interface StatelessComponent<P = {}>'
        Optional<TypeScriptObjectType> objectType =
                PsiTreeUtil.findChildrenOfType(psiElement, TypeScriptObjectType.class).stream()
                .filter(type -> PsiTreeUtil.getTopmostParentOfType(type, TypeScriptTypeParameterList.class) == null)
                .findFirst();

        if (!objectType.isPresent()) return Optional.empty();
        return (isValidTypeScriptObject(objectType.get())) ? objectType : Optional.empty();
    }

    /**
     *
     * @param typeScriptObjectType The {@code TypeScriptObjectType} to check.
     * @return {@code true} if the supplied {@code TypeScriptObjectType} contains at least 1 property.
     */
    public static boolean isValidTypeScriptObject(@NotNull TypeScriptObjectType typeScriptObjectType) {
        return typeScriptObjectType.getTypeMembers().length > 0;
    }

    public static Optional<TypeScriptUnionOrIntersectionType> getTypeScriptUnionOrIntersectionObject(@NotNull PsiElement psiElement) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(psiElement, TypeScriptUnionOrIntersectionType.class));
    }

    /**
     * Checks if the  supplied {@code PsiElement} is within a {@code TypeScriptObject} defined by the element being
     * within a {@code TypeScriptVariable} scope.
     *
     * @param psiElement The {@code PsiElement} to check.
     * @return {@code true} if the supplied {@code PsiElement} is within a {@code TypeScriptVariable}
     */
    public static boolean isInTypeScriptObjectScope(@NotNull PsiElement psiElement) {
        return PsiTreeUtil.getTopmostParentOfType(psiElement, TypeScriptVariable.class) != null;
    }


    /**
     * Collects a {@code List} of distinct resolvable reference names which is used to create hyperlinks in the documentation.
     * Consider the example below where {@code Person} is the only resolvable type which is then replaced with
     * a hyperlink for navigation. The returned {@code List} will therefore only contain "Person" allowing the hyperlink
     * replacement to occur.
     *
     * <p>
     *     {@code gps: Map<string, Person>}
     * </p>
     *
     * @param root The {@code PsiElement} to begin searching for elements of type {@code JSReferenceExpression}.
     * @return The {@code List} of resolvable type names.
     */
    public static List<String> collectResolvableReferenceNames(@NotNull PsiElement root) {
        Predicate<JSReferenceExpression> permitTypeFilter = reference -> {
            if (reference.resolve() == null) return false;
            if (reference.resolve() instanceof TypeScriptTypeParameter) return false;
            if (reference.resolve() instanceof TypeScriptMappedTypeParameter) return false;

            // it is possible primitive types resolve to their TypeScript wrapper such as Boolean which is to be avoided.
            boolean isPrimitiveByConvention = !StringUtil.isCapitalized(reference.getReferenceName());
            return !isPrimitiveByConvention;
        };

        return PsiTreeUtil.collectElementsOfType(root, JSReferenceExpression.class).stream()
                .filter(permitTypeFilter)
                .map(PsiQualifiedReference::getReferenceName)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Collects all generic type parameters. The example below would return [Y, T, P]
     *
     * <pre>
     *     interface Counter<Y> {
     *       interval: Y;
     *       identity<T extends number, P>(arg: T, arg2: P): Y
     *    }
     * </pre>
     *
     * @param root The {@code PsiElement} to begin searching for elements of type {@code TypeScriptTypeParameterList}.
     * @return The list of generic type parameters.
     */
    public static List<String> collectGenericTypeParameters(@NotNull PsiElement root) {
        return PsiTreeUtil.collectElementsOfType(root, TypeScriptTypeParameterList.class).stream()
                .map(TypeScriptTypeParameterList::getTypeParameters)
                .flatMap(Arrays::stream)
                .map(PsiNamedElement::getName)
                .collect(Collectors.toList());
    }
}
