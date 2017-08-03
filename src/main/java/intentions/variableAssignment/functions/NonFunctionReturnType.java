package intentions.variableAssignment.functions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class NonFunctionReturnType extends FunctionValue {

    public NonFunctionReturnType(String signature, List<String> actualGenericTypeValues) {
        super(signature, actualGenericTypeValues);
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public List<String> getAllReturnTypes() {
        return Arrays.asList(signature);
    }

    @Override
    public List<String> getActualGenericTypeValues() {
        // There cant be generic types
        return Collections.emptyList();
    }
}
