package documentation.objectProperties;

import java.util.List;

/**
 * Groups the various types of object properties into categories to simplify the process of displaying nicely formatted
 * documentation.
 *
 * Created by matt on 27-Jun-17.
 */
public class TypeScriptObjectPropertyGroup {
    private List<TypeScriptObjectProperty> propertySignatures;
    private List<TypeScriptObjectProperty> indexableProperties;
    private List<TypeScriptObjectProperty> hybridFunctionProperties;
    private List<TypeScriptObjectProperty> functionProperties;

    private TypeScriptObjectPropertyGroup(List<TypeScriptObjectProperty> propertySignatures,
                                          List<TypeScriptObjectProperty> indexableProperties,
                                          List<TypeScriptObjectProperty> hybridFunctionProperties,
                                          List<TypeScriptObjectProperty> functionProperties) {
        this.propertySignatures = propertySignatures;
        this.indexableProperties = indexableProperties;
        this.hybridFunctionProperties = hybridFunctionProperties;
        this.functionProperties = functionProperties;
    }

    public static class Builder {
        private List<TypeScriptObjectProperty> propertySignatures;
        private List<TypeScriptObjectProperty> indexableProperties;
        private List<TypeScriptObjectProperty> hybridFunctionProperties;
        private List<TypeScriptObjectProperty> functionProperties;

        public Builder propertySignatures(List<TypeScriptObjectProperty> properties) {
            this.propertySignatures = properties;
            return this;
        }

        public Builder indexableProperties(List<TypeScriptObjectProperty> properties) {
            this.indexableProperties = properties;
            return this;
        }

        public Builder hybridFunctionProperties(List<TypeScriptObjectProperty> properties) {
            this.hybridFunctionProperties = properties;
            return this;
        }

        public Builder functionProperties(List<TypeScriptObjectProperty> properties) {
            this.functionProperties = properties;
            return this;
        }

        public TypeScriptObjectPropertyGroup build() {
            return new TypeScriptObjectPropertyGroup(propertySignatures, indexableProperties, hybridFunctionProperties, functionProperties);
        }
    }

    public List<TypeScriptObjectProperty> getPropertySignatures() {
        return propertySignatures;
    }

    public List<TypeScriptObjectProperty> getIndexableProperties() {
        return indexableProperties;
    }

    public List<TypeScriptObjectProperty> getHybridFunctionProperties() {
        return hybridFunctionProperties;
    }

    public List<TypeScriptObjectProperty> getFunctionProperties() {
        return functionProperties;
    }
}
