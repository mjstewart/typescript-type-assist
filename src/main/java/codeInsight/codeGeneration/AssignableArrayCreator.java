package codeInsight.codeGeneration;

import codeInsight.instructions.ArrayInsertInstruction;
import com.intellij.openapi.util.text.StringUtil;
import documentation.types.TypeDescription;
import documentation.objectProperties.TypeScriptObjectProperty;
import settings.TypeAssistApplicationSettings;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Creates an array and assigns the generated code to a variable.
 *
 * Created by matt on 29-May-17.
 */
public class AssignableArrayCreator extends AssignableObjectCreator {

    private ArrayInsertInstruction insertInstruction;
    private TypeDescription typeDescription;
    private TypeAssistApplicationSettings typeAssistApplicationSettings;

    public AssignableArrayCreator(List<TypeScriptObjectProperty> objectPropertyList,
                                  ArrayInsertInstruction insertInstruction,
                                  TypeDescription typeDescription,
                                  TypeAssistApplicationSettings typeAssistApplicationSettings) {
        super(objectPropertyList, typeDescription, typeAssistApplicationSettings);
        this.insertInstruction = insertInstruction;
        this.typeDescription = typeDescription;
        this.typeAssistApplicationSettings = typeAssistApplicationSettings;
    }

    @Override
    public String generate() {
        if (objectPropertyList.size() == 0) return "There are no properties to generate";

        // Code to add the the prefix and suffix of array contents.
        Supplier<String> preArrayBody = () -> new StringBuilder(typeAssistApplicationSettings.VARIABLE_DECLARATION.getCode())
                .append(" ").append(StringUtil.decapitalize(StringUtil.pluralize(typeDescription.getTypeNameWithoutGenerics()))).append(": ")
                .append(CodeGenerator.wrapInQuotesIfGeneric(typeDescription.getTypeName())).append("[]")
                .append(" = [\n").toString();
        Supplier<String> postArrayBody = () -> "];\n";

        // Generate single object
        Supplier<String> generateSingleObject = () -> {
            Supplier<String> preCodePerObject = () -> "{\n";
            Supplier<String> postCodePerObject = () -> "}";
            return objectPropertyList.stream().collect(new CodeGeneratorCollector(preCodePerObject, postCodePerObject, typeAssistApplicationSettings));
        };

        String generatedObjects = Stream.generate(generateSingleObject)
                .limit(insertInstruction.getSize())
                .collect(Collectors.joining(",\n", "", typeAssistApplicationSettings.TRAILING_COMMAS ? ",\n" : "\n"));

        return new StringBuilder()
                .append(preArrayBody.get())
                .append(generatedObjects)
                .append(postArrayBody.get())
                .toString();
    }
}
