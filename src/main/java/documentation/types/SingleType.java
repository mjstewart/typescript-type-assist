package documentation.types;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptSingleType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Wraps a {@code TypeScriptSingleType} which is something like {@code Tree<T> or Person, its simply just a type
 * by itself.}
 *
 * Created by matt on 14-Jun-17.
 */
public class SingleType implements DescribableType {

    private TypeScriptSingleType typeScriptSingleType;

    private SingleType(TypeScriptSingleType typeScriptSingleType) {
        this.typeScriptSingleType = typeScriptSingleType;
    }

    public static Optional<SingleType> of(@NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptSingleType.class))
                .map(SingleType::new);
    }

    @Override
    public String getType() {
        return typeScriptSingleType.getText();
    }
}
