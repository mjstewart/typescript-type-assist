package codeInsight.codeGeneration;

import codeInsight.instructions.InsertInstruction;
import documentation.objectProperties.TypeScriptObjectProperty;
import settings.TypeAssistApplicationSettings;

import java.util.List;
import java.util.function.Supplier;

/**
 * Creates a single object that can be assigned to a property within an existing object.
 *
 * Created by matt on 29-May-17.
 */
public class PropertyObjectCreator implements CodeGenerator {

    private List<TypeScriptObjectProperty> objectPropertyList;
    private InsertInstruction insertInstruction;
    private TypeAssistApplicationSettings typeAssistApplicationSettings;

    public PropertyObjectCreator(List<TypeScriptObjectProperty> objectPropertyList,
                                 InsertInstruction insertInstruction,
                                 TypeAssistApplicationSettings typeAssistApplicationSettings) {
        this.objectPropertyList = objectPropertyList;
        this.insertInstruction = insertInstruction;
        this.typeAssistApplicationSettings = typeAssistApplicationSettings;
    }

    @Override
    public String generate() {
        if (objectPropertyList.size() == 0) return "There are no properties to generate";

        Supplier<String> preCode = () -> "{\n";
        Supplier<String> postCode = () -> {
            if (insertInstruction.isLastProperty()) {
                if (typeAssistApplicationSettings.TRAILING_COMMAS) {
                    return "},\n";
                }
                return "}\n";
            }
            return "},\n";
        };

        return objectPropertyList.stream().collect(new CodeGeneratorCollector(preCode, postCode, typeAssistApplicationSettings));
    }
}
