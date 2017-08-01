package intentions.variableAssignment.functions;

import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;

public class StandardFunction implements FunctionValue {
    private TypeScriptFunction typeScriptFunction;

    public StandardFunction(TypeScriptFunction typeScriptFunction) {
        this.typeScriptFunction = typeScriptFunction;
    }

    private String parameters() {
        return typeScriptFunction.getParameterList() == null ? ""
                : typeScriptFunction.getParameterList().getText();
    }

    @Override
    public String returnType() {
        return typeScriptFunction.getReturnTypeElement() == null ? "any"
                : typeScriptFunction.getReturnTypeElement().getText();
    }

    @Override
    public String signature() {
        return typeScriptFunction.getText();
    }

    @Override
    public String toString() {
        return signature();
    }
}
