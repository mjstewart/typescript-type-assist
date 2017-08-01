package intentions.variableAssignment;

import com.intellij.openapi.util.Pair;

/**
 * T -> string
 */
public class GenericParameterPair {
    private String genericType;
    private String genericTypeValue;

    public GenericParameterPair(String genericType, String genericTypeValue) {
        this.genericType = genericType;
        this.genericTypeValue = genericTypeValue;
    }

    @Override
    public String toString() {
        return genericType + " -> " + genericTypeValue;
    }

    /**
     * @return Pair.1 contains the regex search key, Pair.2 is the replacement value.
     */
    public Pair<String, String> getReplacementInstruction() {
        return Pair.create(wordBoundary(genericType), genericTypeValue);
    }

    private String wordBoundary(String value) {
        return "\\b" + value + "\\b";
    }
}
