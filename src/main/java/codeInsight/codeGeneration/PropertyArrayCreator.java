package codeInsight.codeGeneration;

import codeInsight.instructions.ArrayInsertInstruction;
import documentation.objectProperties.TypeScriptObjectProperty;
import settings.TypeAssistApplicationSettings;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates an array for a property in an existing object.
 *
 * Created by matt on 29-May-17.
 */
public class PropertyArrayCreator implements CodeGenerator {

    private List<TypeScriptObjectProperty> objectPropertyList;
    private ArrayInsertInstruction insertInstruction;
    private TypeAssistApplicationSettings typeAssistApplicationSettings;

    public PropertyArrayCreator(List<TypeScriptObjectProperty> objectPropertyList,
                                ArrayInsertInstruction insertInstruction,
                                TypeAssistApplicationSettings typeAssistApplicationSettings) {
        this.objectPropertyList = objectPropertyList;
        this.insertInstruction = insertInstruction;
        this.typeAssistApplicationSettings = typeAssistApplicationSettings;
    }

    @Override
    public String generate() {
        if (objectPropertyList.size() == 0) return "There are no properties to generate";

        // Code to add the the prefix and suffix of array contents.
        Supplier<String> preArrayBody = () -> "[\n";
        Supplier<String> postArrayBody = () -> {
            if (insertInstruction.isLastProperty()) {
                if (typeAssistApplicationSettings.TRAILING_COMMAS) {
                    return "\n],\n";
                }
                return "\n]\n";
            }
            return "\n],\n";
        };

        // Generate single object
        Supplier<String> generateSingleObject = () -> {
            Supplier<String> preCodePerObject = () -> "{\n";
            Supplier<String> postCodePerObject = () -> "}";
            return objectPropertyList.stream().collect(new CodeGeneratorCollector(preCodePerObject, postCodePerObject, typeAssistApplicationSettings));
        };

        String generatedObjects = Stream.generate(generateSingleObject)
                .limit(insertInstruction.getSize())
                .collect(Collectors.joining(",\n", "", typeAssistApplicationSettings.TRAILING_COMMAS ? "," : ""));

        return new StringBuilder()
                .append(preArrayBody.get())
                .append(generatedObjects)
                .append(postArrayBody.get())
                .toString();
    }
}
