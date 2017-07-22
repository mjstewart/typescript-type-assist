package documentation.objectProperties;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptCallSignature;

import java.util.Optional;

/**
 * Hybrid type based on official TypeScript docs.
 *
 * <pre>
 * interface Counter {
 *   (start: number): string;     <---- We are targeting this which is a TypeScriptCallSignature
 *   interval: number;
 *   reset(): void;
 * }
 *
 * function getCounter(): Counter {
 *     let counter = <Counter>function (start: number) { };
 *     counter.interval = 123;
 *     counter.reset = function () { };
 *     return counter;
 * }
 * </pre>
 *
 * Created by matt on 27-Jun-17.
 */
public class HybridFunctionProperty extends TypeScriptObjectProperty {
    private TypeScriptCallSignature callSignature;

    public HybridFunctionProperty(TypeScriptCallSignature callSignature) {
        this.callSignature = callSignature;
    }

    @Override
    public String getType() {
        return getPropertyType();
    }

    @Override
    public Optional<String> getDocumentationPropertyName() {
        // Hybrid type doesn't have a property name, instead the full property type is displayed.
        return Optional.empty();
    }

    @Override
    public Optional<String> getCodeGenPropertyName() {
        return Optional.empty();
    }

    @Override
    public String getPropertyType() {
        return toFullSignature(callSignature);
    }

    @Override
    public boolean isOptional() {
        return callSignature.isOptional();
    }
}
