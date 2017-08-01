package intentions.variableAssignment;

import com.intellij.lang.javascript.psi.JSCallExpression;
import intentions.variableAssignment.functions.FunctionValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Given a return type of {@code (a: A) => (b: B) => (c: C) => D}
 * <p>
 * <p>ResolvedFunctionTypes contains a list of all the possible return values to cater for partially applied
 * function variations.</p>
 * <p>
 * <pre>
 * (a: A) => (b: B) => (c: C) => D
 * (b: B) => (c: C) => D
 * (c: C) => D
 * </pre>
 * <p>
 * If this example function is partially applied 3 times, the final return value is D.
 */
public abstract class ResolvedFunctionTypes {
    protected List<FunctionValue> functionValues;

    public ResolvedFunctionTypes() {
        functionValues = new ArrayList<>();
    }

    public List<FunctionValue> getFunctionValues() {
        return functionValues;
    }

    public FunctionValue getLast() {
        return functionValues.get(functionValues.size() - 1);
    }

    public abstract String getReturnType(List<CallExpressionWithArgs> callExpressions);
}
