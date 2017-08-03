package intentions.variableAssignment.functions;

import java.util.List;

/**
 * A function type is a regular typescript function.
 *
 * <pre>
 *     function print()...
 * </pre>
 */
public class FunctionType extends FunctionValue {

    public FunctionType(String signature, List<String> genericTypes) {
        super(signature, genericTypes);
    }

    @Override
    public List<String> getAllReturnTypes() {
        return getAllReturnTypes(signature);
    }

    @Override
    public List<String> getActualGenericTypeValues() {
        return actualGenericTypeValues;
    }

    @Override
    public String getSignature() {
         return signature;
    }

    @Override
    public String toString() {
        return getSignature();
    }
}
