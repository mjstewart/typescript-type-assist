package documentation.types;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptLiteralType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * An example of a literal type is a single value such as {@code type Frequency = 22} or {@code type Day = 'sunday'}
 * <p>
 * Created by matt on 28-Jun-17.
 */
public class LiteralType implements DescribableType {

    private TypeScriptLiteralType literalType;

    private LiteralType(TypeScriptLiteralType literalType) {
        this.literalType = literalType;
    }

    @Override
    public String getType() {
        return literalType.getText();
    }

    public static Optional<LiteralType> of(@NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptLiteralType.class))
                .map(LiteralType::new);
    }
}
