package intentions.variableAssignment;

import com.intellij.lang.javascript.psi.JSCallExpression;

/**
 * The internal {@code JSCallExpression} must have a non null argument list which is checked by caller prior to
 * instantiating.
 *
 * <p>A {@code CallExpressionWithArgs} is the function arguments such as {@code concat("hello", "world") where the
 * arguments are {@code ("hello", "world")}.</p>
 */
public class CallExpressionWithArgs {
    private JSCallExpression jsCallExpression;

    public CallExpressionWithArgs(JSCallExpression jsCallExpression) {
        this.jsCallExpression = jsCallExpression;
    }

    public String getArguments() {
        if (jsCallExpression.getArgumentList() == null) {
            return "";
        }
        return jsCallExpression.getArgumentList().getText();
    }

    public JSCallExpression getJsCallExpression() {
        return jsCallExpression;
    }

    @Override
    public String toString() {
        return getArguments();
    }
}
