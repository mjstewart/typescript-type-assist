package documentation.types;

import codeInsight.TypeAssistPsiUtil;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptType;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptUnionOrIntersectionType;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Wraps a TypeScript union or intersection type providing methods to easily extract key documentation information.
 *
 * Created by matt on 05-Jun-17.
 */
public class UnionOrIntersectionType implements DescribableType {
    private TypeScriptType typeScriptType;

    private UnionOrIntersectionType(TypeScriptType typeScriptType) {
        this.typeScriptType = typeScriptType;
    }

    @Override
    public String getType() {
        return typeScriptType.getText();
    }

    public static Optional<List<UnionOrIntersectionType>> of(@NotNull PsiElement element) {
        return TypeAssistPsiUtil.getTypeScriptUnionOrIntersectionObject(element)
                .map(TypeScriptUnionOrIntersectionType::getTypes)
                .map(Arrays::stream)
                .map(typeStream -> typeStream.map(UnionOrIntersectionType::new))
                .map(propertyInfoStream -> propertyInfoStream.collect(Collectors.toList()));
    }
}
