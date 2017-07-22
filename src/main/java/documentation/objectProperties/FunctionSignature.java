package documentation.objectProperties;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunctionSignature;

import java.util.Optional;

/**
 * Wraps a {@code TypeScriptFunctionSignature} such as {@code toString(person: Person): string}
 * <p>
 * Created by matt on 29-Jun-17.
 */
public class FunctionSignature extends TypeScriptObjectProperty {

    private TypeScriptFunctionSignature functionSignature;

    public FunctionSignature(TypeScriptFunctionSignature functionSignature) {
        this.functionSignature = functionSignature;
    }

    @Override
    public String getType() {
        return getPropertyType();
    }

    @Override
    public Optional<String> getDocumentationPropertyName() {
        return Optional.of(getPropertyType());
    }

    @Override
    public Optional<String> getCodeGenPropertyName() {
        return Optional.empty();
    }

    @Override
    public String getPropertyType() {
        return toFullSignature(functionSignature);
    }

    @Override
    public boolean isOptional() {
        return false;
    }


}
