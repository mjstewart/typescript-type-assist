package documentation.types;

import codeInsight.TypeAssistPsiUtil;
import com.intellij.lang.javascript.psi.JSFunction;
import com.intellij.lang.javascript.psi.ecma6.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import documentation.TypeAssistDocumentationProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * A {@code TypeDescription} provides extra context such as the specific kind of TypeScript type the
 * resolved {@code PsiElement} is such as interface or union type etc.
 *
 * <p>A {@code TypeDescription} instructs {@link TypeAssistDocumentationProvider#generateDoc} on how to generate the
 * concrete documentation based on the {@code TypeDefinition}</p>
 *
 * Created by matt on 05-Jun-17.
 */
public class TypeDescription {
    private String typeName;
    private TypeDefinition typeDefinition;
    private PsiElement resolvedElement;

    private TypeDescription(String typeName, TypeDefinition typeDefinition, PsiElement resolvedElement) {
        this.typeName = typeName;
        this.typeDefinition = typeDefinition;
        this.resolvedElement = resolvedElement;
    }

    public static TypeDescription none() {
        return new TypeDescription(null, TypeDefinition.None, null);
    }

    /**
     * This method determines the specific type of the TypeScript PsiElement and maps it to one of the supported
     * {@code TypeDefinition}s which ultimately determines how the documentation is presented in
     * {@code TypeAssistDocumentationProvider}.
     *
     * <p>The supplied {@code PsiElement} must be resolved, meaning make sure the resolvedElement is not the original
     * element sitting in the source code where the caret is currently on which is most likely a JS lexer token which
     * is of no use.</p>
     *
     * @param resolvedElement The resolved {@code PsiElement}
     * @return The {@code TypeDescription}.
     */
    public static TypeDescription create(@NotNull PsiElement resolvedElement) {
        if (resolvedElement instanceof TypeScriptInterface) {
            TypeScriptInterface resolvedInterface = (TypeScriptInterface) resolvedElement;
            String fullTypeName = createFullTypeName(resolvedInterface.getName(), resolvedInterface.getTypeParameterList());
            return new TypeDescription(fullTypeName, TypeDefinition.Interface, resolvedElement);
        }
        if (resolvedElement instanceof TypeScriptEnum) {
            return new TypeDescription(((TypeScriptEnum)resolvedElement).getName(), TypeDefinition.Enum, resolvedElement);
        }
        if (resolvedElement instanceof TypeScriptTypeAlias) {
            TypeScriptTypeAlias resolvedAlias = (TypeScriptTypeAlias) resolvedElement;
            String fullTypeName = createFullTypeName(resolvedAlias.getName(), resolvedAlias.getTypeParameterList());

            for (PsiElement childElement : resolvedElement.getChildren()) {
                // Cannot use PsiTreeUtil.getChildType as we do not want to search nested subtypes.

                if (childElement instanceof TypeScriptUnionOrIntersectionType) {
                    TypeScriptUnionOrIntersectionType unionOrIntersectionType = (TypeScriptUnionOrIntersectionType) childElement;
                    if (unionOrIntersectionType.isUnionType()) {
                        return new TypeDescription(fullTypeName, TypeDefinition.Union, resolvedElement);
                    }
                    if (unionOrIntersectionType.isIntersectionType()) {
                        return new TypeDescription(fullTypeName, TypeDefinition.Intersection, resolvedElement);
                    }
                }
                if (childElement instanceof TypeScriptObjectType) {
                    TypeScriptObjectType typeScriptObjectType = (TypeScriptObjectType) childElement;
                    if (TypeAssistPsiUtil.isValidTypeScriptObject(typeScriptObjectType)) {
                        return new TypeDescription(fullTypeName, TypeDefinition.TypeAliasObject, resolvedElement);
                    }
                }
                if (childElement instanceof TypeScriptLiteralType) {
                    return new TypeDescription(fullTypeName, TypeDefinition.TypeAliasLiteral, resolvedElement);
                }
                if (childElement instanceof TypeScriptFunctionType) {
                    return new TypeDescription(fullTypeName, TypeDefinition.TypeAliasFunction, resolvedElement);
                }
                if (childElement instanceof TypeScriptSingleType) {
                    return new TypeDescription(fullTypeName, TypeDefinition.TypeAliasSingle, resolvedElement);
                }
                if (childElement instanceof TypeScriptMappedType) {
                    return new TypeDescription(fullTypeName, TypeDefinition.TypeAliasMappedType, resolvedElement);
                }
            }
        }
        return none();
    }

    public boolean isValid() {
        return typeName != null && typeDefinition != TypeDefinition.None && resolvedElement != null;
    }

    public TypeDefinition getTypeDefinition() {
        return typeDefinition;
    }

    public PsiElement getResolvedElement() {
        return resolvedElement;
    }

    public String getTypeName() {
        return typeName;
    }

    /**
     * When auto code is generated, the variable name should not include generics which is what {@link #getTypeName}
     * returns.
     *
     * @return The type name without generics.
     */
    public String getTypeNameWithoutGenerics() {
        // Whatever comes before the first < will be the variable name since beyond that is generic parameters.
        return getTypeName().split("<")[0];
    }

    /**
     * If the interface {@code typeName} is 'Address' and {@code parameterList} is {@code <B extends string>}
     * then the returned value is {@code Address<B extends string>}
     */
    private static String createFullTypeName(String typeName, TypeScriptTypeParameterList parameterList) {
        return typeName + ((parameterList == null) ? "" : parameterList.getText());
    }
}