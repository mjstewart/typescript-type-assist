package intentions.variableAssignment.functions;

import java.util.List;

/**
 * A function expression is a lambda.
 *
 * <pre>
 *     const addMe = (a: number) => (b: number): number => a + b
 * </pre>
 */
public class FunctionExpression extends FunctionValue {

    public FunctionExpression(String expression, List<String> genericTypes) {
        super(expression, genericTypes);
    }

    @Override
    public String getSignature() {
        /*
         * An example of a function expression is
         * const addMe = (a: number) => (b: number): number => a + b;
         *               [           function expression            ]
         *
         * It needs to be formatted into a normal type signature.
         * 1. Transform '(b: number): number' into is transformed into '(b: number) => number'
         * 2. Remove the expression to the right of the last => which is 'a + b'
         */
        String formatted = signature.replaceAll("\\):", ") =>");
        return formatted.substring(0, formatted.lastIndexOf("=>")).trim();
    }

    @Override
    public List<String> getAllReturnTypes() {
        return getAllReturnTypes(getSignature());
    }

    @Override
    public List<String> getActualGenericTypeValues() {
        return actualGenericTypeValues;
    }

    @Override
    public String toString() {
        return getSignature();
    }
}
