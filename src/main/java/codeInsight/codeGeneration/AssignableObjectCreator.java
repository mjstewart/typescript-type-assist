package codeInsight.codeGeneration;

import com.intellij.openapi.util.text.StringUtil;
import documentation.objectProperties.TypeScriptObjectProperty;
import documentation.types.TypeDescription;
import settings.TypeAssistApplicationSettings;

import java.util.List;
import java.util.function.Supplier;

/**
 * Creates single objects that are assigned to a variable.
 *
 * Created by matt on 29-May-17.
 */
public class AssignableObjectCreator implements CodeGenerator {
    protected List<TypeScriptObjectProperty> objectPropertyList;
    private TypeDescription typeDescription;
    private TypeAssistApplicationSettings typeAssistApplicationSettings;

    public AssignableObjectCreator(List<TypeScriptObjectProperty> objectPropertyList,
                                   TypeDescription typeDescription,
                                   TypeAssistApplicationSettings typeAssistApplicationSettings) {
        this.objectPropertyList = objectPropertyList;
        this.typeDescription = typeDescription;
        this.typeAssistApplicationSettings = typeAssistApplicationSettings;
    }

    @Override
    public String generate() {
        if (objectPropertyList.isEmpty()) return "There are no properties to generate";

        Supplier<String> preCode = () -> new StringBuilder(typeAssistApplicationSettings.VARIABLE_DECLARATION.getCode())
                .append(" ").append(StringUtil.decapitalize(typeDescription.getTypeNameWithoutGenerics())).append(": ")
                .append(CodeGenerator.wrapInQuotesIfGeneric(typeDescription.getTypeName()))
                .append(" = {\n").toString();

        Supplier<String> postCode = () -> typeAssistApplicationSettings.END_WITH_SEMI_COLON ? "};\n" : "}\n";

        return objectPropertyList.stream().collect(new CodeGeneratorCollector(preCode, postCode, typeAssistApplicationSettings));
    }
}
