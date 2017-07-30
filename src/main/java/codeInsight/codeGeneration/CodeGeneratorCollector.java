package codeInsight.codeGeneration;

import documentation.objectProperties.TypeScriptObjectProperty;
import settings.TypeAssistApplicationSettings;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * Collects all {@code TypeScriptObjectProperty} and converts into valid TypeScript code.
 *
 * Created by matt on 29-May-17.
 */
public class CodeGeneratorCollector implements Collector<TypeScriptObjectProperty, StringBuilder, String> {

    private final String MISSING_PROPERTY_NAME = "__auto_generated_missing";

    private final Supplier<String> preCode;
    private final Supplier<String> postCode;
    private TypeAssistApplicationSettings typeAssistApplicationSettings;

    /**
     *
     * @param preCode Code to insert before collecting property signatures.
     * @param postCode Code to insert after collecting property signatures.
     * @param typeAssistApplicationSettings Plugin settings.
     */
    public CodeGeneratorCollector(Supplier<String> preCode,
                                  Supplier<String> postCode,
                                  TypeAssistApplicationSettings typeAssistApplicationSettings) {
        this.preCode = preCode;
        this.postCode = postCode;
        this.typeAssistApplicationSettings = typeAssistApplicationSettings;
    }

    @Override
    public Supplier<StringBuilder> supplier() {
        return StringBuilder::new;
    }

    @Override
    public BiConsumer<StringBuilder, TypeScriptObjectProperty> accumulator() {
        return (sb, propertyValue) -> {
            // Turn into a js string so the user knows the types they need to implement. Additional tooling will use
            // this as a marker to draw highlights etc.

            String type;
            if (propertyValue.getPropertyType().contains(" ")) {
                /*
                 * Could be a union type which requires all values to be wrapped in a single string for highlighting.
                 * Its simpler to escape string using double quote which relies upon only single quotes existing.
                 */
                type = propertyValue.getPropertyType().replaceAll("[\"]", "'");
                type = wrapInQuotes(type);
            } else {
                type = wrapInQuotes(propertyValue.getPropertyType());
            }

            String propertyName = propertyValue.getCodeGenPropertyName().orElse(MISSING_PROPERTY_NAME);
            sb.append(propertyName).append(": ").append(type).append(",").append("\n");
        };
    }

    @Override
    public BinaryOperator<StringBuilder> combiner() {
        return StringBuilder::append;
    }

    @Override
    public Function<StringBuilder, String> finisher() {
        return generatedCodeBuilder -> {
            String generatedCode = generatedCodeBuilder.toString();
            generatedCode = typeAssistApplicationSettings.TRAILING_COMMAS ? generatedCode :
                    CodeGenerator.removeDanglingComma(generatedCode);

            return new StringBuilder()
                    .append(preCode.get())
                    .append(generatedCode)
                    .append(postCode.get())
                    .toString();
        };
    }

    @Override
    public Set<Characteristics> characteristics() {
        return EnumSet.noneOf(Characteristics.class);
    }

    private String wrapInQuotes(String value) {
        return String.format("%s%s%s", "\"", value, "\"");
    }
}