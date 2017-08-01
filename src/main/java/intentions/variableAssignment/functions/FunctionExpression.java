package intentions.variableAssignment.functions;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;

public class FunctionExpression implements FunctionValue {
    private TypeScriptFunction expression;

    public FunctionExpression(TypeScriptFunction expression) {
        this.expression = expression;
    }

    @Override
    public String signature() {


        return expression.getText().replaceAll("\\):", ") =>");
//        int lastIndex = expression.getText().lastIndexOf("=>");
//        return expression.getText().substring(0, lastIndex).trim();
    }

    @Override
    public String returnType() {
        return "";
    }



    @Override
    public String toString() {
        return signature();
    }
}
