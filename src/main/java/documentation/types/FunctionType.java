package documentation.types;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunctionType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * Example: type NameResolver = () => string;
 *
 * Created by matt on 28-Jun-17.
 */
public class FunctionType implements DescribableType {

    private TypeScriptFunctionType functionType;

    private FunctionType(TypeScriptFunctionType functionType) {
        this.functionType = functionType;
    }

    @Override
    public String getType() {
        return functionType.getText();
    }

    public static Optional<FunctionType> of(@NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptFunctionType.class))
                .map(FunctionType::new);
    }
}
