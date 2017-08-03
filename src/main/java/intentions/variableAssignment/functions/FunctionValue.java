package intentions.variableAssignment.functions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public abstract class FunctionValue {
    protected String signature;
    protected List<String> actualGenericTypeValues;

    public FunctionValue(String signature, List<String> actualGenericTypeValues) {
        this.signature = signature;
        this.actualGenericTypeValues = actualGenericTypeValues;
    }

    public abstract String getSignature();
    public abstract List<String> getAllReturnTypes();
    public abstract List<String> getActualGenericTypeValues();

    /**
     * Splits the signature by {@code =>} and puts all possible return types in a list.
     *
     * <pre>
     *     Given signature = (t: T, a: A) => (c: A) => (d: T) => string
     *
     *     returned list is
     *
     *     (t: T, a: A) => (c: A) => (d: T) => string
     *     (c: A) => (d: T) => string
     *     (d: T) => string
     *     string
     * </pre>
     */
    public List<String> getAllReturnTypes(String signature) {
        if (signature == null) return Collections.emptyList();

        List<String> callExpressions = Arrays.stream(signature.split("=>"))
                .map(String::trim).collect(Collectors.toList());

        return IntStream.range(0, callExpressions.size())
                .mapToObj(i -> callExpressions.subList(i, callExpressions.size()).stream().collect(Collectors.joining(" => ")))
                .collect(Collectors.toList());
    }
}
