package documentation.types;

/**
 * The types available for documentation. The description is displayed in the UI to describe the type in greater detail.
 *
 * <p>
 * Created by matt on 05-Jun-17.
 */
public enum TypeDefinition {
    Interface("interface"),
    TypeAliasObject("type alias for object"),
    TypeAliasLiteral("type alias for literal type"),
    TypeAliasFunction("type alias for function type"),
    TypeAliasSingle("type alias for single type"),
    TypeAliasMappedType("type alias mapped type"),
    Union("union type"),
    Intersection("intersection type"),
    Enum("enum"),
    None("Missing type");

    String description;

    TypeDefinition(String description) {
        this.description = description;
    }

    /**
     * Get the description to display for the title in any user interface.
     *
     * @return The description associated with the type definition.
     */
    public String getDescription() {
        return description;
    }
}
