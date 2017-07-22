package documentation.types;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptMappedType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

/**
 * <pre>
 * type Readonly<T> = {
 *    readonly [P in keyof T]: T[P];  <-- is the mapped type which is within a type alias.
 * }
 * </pre>
 *
 * Created by matt on 28-Jun-17.
 */
public class MappedType implements DescribableType {

    private TypeScriptMappedType mappedType;

    private MappedType(TypeScriptMappedType mappedType) {
        this.mappedType = mappedType;
    }


    public static Optional<MappedType> of(@NotNull PsiElement element) {
        return Optional.ofNullable(PsiTreeUtil.findChildOfType(element, TypeScriptMappedType.class))
                .map(MappedType::new);
    }

    @Override
    public String getType() {
        // Since there can only be 1 mapped property within the type alias, extract the value within the object.
        return mappedType.getText().replaceAll("[{};]", "").trim();
    }
}
