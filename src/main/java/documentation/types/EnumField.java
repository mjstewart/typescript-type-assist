package documentation.types;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptField;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Describes a {@code TypeScriptEnum} by extracting its enum value.
 *
 * Created by matt on 05-Jun-17.
 */
public class EnumField implements DescribableType {

    // Represents a single enum value.
    final private TypeScriptField typeScriptField;

    private EnumField(TypeScriptField typeScriptField) {
        this.typeScriptField = typeScriptField;
    }

    /**
     * Collects all {@code EnumField}s within the supplied {@code PsiElement}.
     *
     * @param element The element containing enum types.
     * @return The list of extracted {@code EnumField}s
     */
    public static Optional<List<EnumField>> of(@NotNull PsiElement element) {
        return Optional.of(PsiTreeUtil.findChildrenOfType(element, TypeScriptField.class))
                .map(Collection::stream)
                .map(typeScriptFieldStream -> typeScriptFieldStream.map(EnumField::new))
                .map(propertyInfoStream -> propertyInfoStream.collect(Collectors.toList()));
    }

    @Override
    public String getType() {
        return typeScriptField.getText();
    }
}
