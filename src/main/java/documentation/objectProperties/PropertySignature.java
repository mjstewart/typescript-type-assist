package documentation.objectProperties;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptPropertySignature;
import documentation.objectProperties.TypeScriptObjectProperty;

import java.util.Optional;

/**
 * Wraps {@code TypeScriptPropertySignature} such as {@code firstName: string}.
 *
 * <p>
 * Created by matt on 06-Jun-17.
 */
public class PropertySignature extends TypeScriptObjectProperty {
    public static final String MISSING_TYPE_DEFAULT = "any";

    private TypeScriptPropertySignature typeScriptPropertySignature;

    public PropertySignature(TypeScriptPropertySignature typeScriptPropertySignature) {
        this.typeScriptPropertySignature = typeScriptPropertySignature;
    }

    @Override
    public String getType() {
        /*
         * Optional is safe as all paths in getDocumentationPropertyName return a valid optional.
         * An Optional needs to be returned as other TypeScriptObjectProperty return empty.
         * The full property getSignature gets transformed by the TypeAssistDocumentationProvider formatter.
         */
        return getDocumentationPropertyName().get() + (isOptional() ? "?" : "") + ": " + getPropertyType();
    }

    @Override
    public Optional<String> getDocumentationPropertyName() {
        if (isReadOnly(typeScriptPropertySignature)) {
            return Optional.of("readonly " + typeScriptPropertySignature.getMemberName());
        }
        return Optional.of(typeScriptPropertySignature.getMemberName());
    }

    @Override
    public Optional<String> getCodeGenPropertyName() {
        // Just the raw property name, no readonly prepended.
        return Optional.of(typeScriptPropertySignature.getMemberName());
    }

    @Override
    public String getPropertyType() {
        if (typeScriptPropertySignature.getTypeDeclaration() == null) {
            return MISSING_TYPE_DEFAULT;
        } else {
            return typeScriptPropertySignature.getTypeDeclaration().getText();
        }
    }

    @Override
    public boolean isOptional() {
        return typeScriptPropertySignature.isOptional();
    }
}
