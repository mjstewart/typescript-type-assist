package intentions.variableAssignment;

import com.intellij.openapi.util.Pair;
import utils.AppUtils;

import java.util.List;

public class ReturnTypeEvaluator2 {
    private OriginalFunctionResult originalFunctionResult;
    private ResolvedFunctionResult resolvedFunctionResult;

    public ReturnTypeEvaluator2(OriginalFunctionResult originalFunctionResult, ResolvedFunctionResult resolvedFunctionResult) {
        this.originalFunctionResult = originalFunctionResult;
        this.resolvedFunctionResult = resolvedFunctionResult;
    }

    public String evaluate() {
        List<Pair<String, String>> genericTypeReplacementPairs =
                AppUtils.zipInto(originalFunctionResult.getActualGenericTypeValues(), resolvedFunctionResult.getGenericTypes(),
                        (genericType, typeValue) -> new GenericParameterPair(genericType, typeValue).getReplacementInstruction());

        System.out.println("ReturnTypeEvaluator2: genericTypeReplacementPairs");
        System.out.println(genericTypeReplacementPairs);

//        String returnType = resolvedFunctionResult.getResolvedStandardFunctions()
//                .map(functionTypes -> functionTypes.getReturnType(originalFunctionResult.getCallExpressions()))
//                .get();
//        String resolvedReturnType = AppUtils.replace(returnType, genericTypeReplacementPairs);

//        System.out.println("ReturnTypeEvaluator2 returnType is: " + resolvedReturnType);
        return "";
    }
}
