package documentation.objectProperties;

import codeInsight.TypeAssistPsiUtil;
import com.intellij.lang.javascript.psi.JSParameterList;
import com.intellij.lang.javascript.psi.JSParameterListElement;
import com.intellij.lang.javascript.psi.JSType;
import com.intellij.lang.javascript.psi.ecma6.*;
import com.intellij.lang.javascript.psi.ecmal4.JSAttributeList;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import documentation.types.DescribableType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A TypeScript interface or more generally a javascript object can consist of many different types such as
 * {@code TypeScriptPropertySignature, TypeScriptIndexSignature, TypeScriptFunction} which all have different
 * ways to represent the property name and type. The TypeScript handbook section under interfaces contains all the
 * different types.
 *
 * Created by matt on 06-Jun-17.
 */
public abstract class TypeScriptObjectProperty implements DescribableType {

    /**
     * Creates all property groups that can be displayed in the documentation including.
     *
     * <ul>
     *     <li>Property signatures</li>
     *     <li>Indexable properties</li>
     *     <li>Hybrid functions</li>
     *     <li>Function signatures</li>
     * </ul>
     *
     * @param element The element which contains a {@code TypeScriptObjectType}
     * @return The {@code TypeScriptObjectPropertyGroup} containing each of the various categories.
     */
    public static Optional<TypeScriptObjectPropertyGroup> of(@NotNull PsiElement element) {
        return TypeAssistPsiUtil.getChildTypeScriptObject(element)
                .map(typeScriptObject -> new TypeScriptObjectPropertyGroup.Builder()
                        .propertySignatures(toPropertySignatures(typeScriptObject))
                        .indexableProperties(toIndexableProperties(typeScriptObject))
                        .hybridFunctionProperties(toHybridFunctionProperties(typeScriptObject))
                        .functionProperties(toFunctionProperties(typeScriptObject))
                        .build());
    }

    private static List<TypeScriptObjectProperty> toPropertySignatures(TypeScriptObjectType typeScriptObjectType) {
        return Stream.of(typeScriptObjectType.getChildren())
                .filter(child -> child instanceof TypeScriptPropertySignature)
                .map(property -> new PropertySignature((TypeScriptPropertySignature) property))
                .collect(Collectors.toList());
    }

    private static List<TypeScriptObjectProperty> toHybridFunctionProperties(TypeScriptObjectType typeScriptObjectType) {
        return Stream.of(typeScriptObjectType.getChildren())
                .filter(child -> child instanceof TypeScriptCallSignature)
                .map(property -> new HybridFunctionProperty((TypeScriptCallSignature) property))
                .collect(Collectors.toList());
    }

    private static List<TypeScriptObjectProperty> toIndexableProperties(TypeScriptObjectType typeScriptObjectType) {
        return Stream.of(typeScriptObjectType.getChildren())
                .filter(child -> child instanceof TypeScriptIndexSignature)
                .map(property -> new IndexableProperty((TypeScriptIndexSignature) property))
                .collect(Collectors.toList());
    }

    private static List<TypeScriptObjectProperty> toFunctionProperties(TypeScriptObjectType typeScriptObjectType) {
        return Stream.of(typeScriptObjectType.getChildren())
                .filter(child -> child instanceof TypeScriptFunctionSignature)
                .map(property -> new FunctionSignature((TypeScriptFunctionSignature) property))
                .collect(Collectors.toList());
    }

    /**
     * Allows the property name to receive specific formatting such as prepending readonly since its not built in
     * at the property name level within the Psi structure.
     *
     * @return The property name as it should be displayed in the documentation.
     */
    public abstract Optional<String> getDocumentationPropertyName();

    /**
     * @return The syntactically valid property name to be inserted during code generation.
     */
    public abstract Optional<String> getCodeGenPropertyName();

    /**
     * @return The type of the property as a {@code String}.
     */
    public abstract String getPropertyType();

    /**
     * @return {@code true} if this property is an optional type.
     */
    public abstract boolean isOptional();

    /**
     * @param propertySignature The {@code TypeScriptPropertySignature} in which to find a readonly attribute in.
     * @return {@code true} if this property is readonly.
     */
    protected boolean isReadOnly(@NotNull PsiElement propertySignature) {
        JSAttributeList attributeList = PsiTreeUtil.findChildOfType(propertySignature, JSAttributeList.class);
        return attributeList != null && attributeList.hasModifier(JSAttributeList.ModifierType.READONLY);
    }

    /**
     * Given a {@code TypeScriptFunction}, construct the full function getSignature. This is done to avoid including
     * the JSDocComment which occurs if you simply just print the text value of the function.
     *
     * @param typeScriptFunction The {@code TypeScriptFunction}
     * @return The full function getSignature.
     */
    protected static String toFullSignature(TypeScriptFunction typeScriptFunction) {
        String functionName = typeScriptFunction.getName() == null ? "" : typeScriptFunction.getName();
        String returnType = typeScriptFunction.getReturnType() == null ? "" :
                ": " + typeScriptFunction.getReturnType().getTypeText(JSType.TypeTextFormat.CODE);

        List<String> paramList = new ArrayList<>();
        JSParameterList parameterList = typeScriptFunction.getParameterList();
        if (parameterList != null) {
            for (JSParameterListElement param : parameterList.getParameters()) {
                paramList.add(param.getText());
            }
        }

        List<String> typeParamList = new ArrayList<>();
        TypeScriptTypeParameterList typeParameterList = typeScriptFunction.getTypeParameterList();
        if (typeParameterList != null) {
            for (TypeScriptTypeParameter param : typeParameterList.getTypeParameters()) {
                typeParamList.add(param.getText());
            }
        }

        String genericTypeParams = typeParamList.isEmpty() ? "" :
                typeParamList.stream().collect(Collectors.joining(", ", "<", ">"));

        return paramList.stream()
                .collect(Collectors.joining(", ", functionName + genericTypeParams + "(", ")" + returnType));
    }
}
