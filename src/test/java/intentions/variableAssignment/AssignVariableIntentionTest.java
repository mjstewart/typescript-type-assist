package intentions.variableAssignment;

import com.intellij.lang.javascript.psi.JSExpressionStatement;
import com.intellij.lang.javascript.psi.JSReferenceExpression;
import com.intellij.lang.javascript.psi.ecma6.TypeScriptFunction;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import intentions.variableAssignment.resolvers.FunctionDeclarationResolveResult;
import intentions.variableAssignment.resolvers.FunctionDeclarationResolver;
import intentions.variableAssignment.resolvers.ResolveUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;

public class AssignVariableIntentionTest extends LightCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/java/intentions/variableAssignment/testData";
    }

    private List<String> callExpressionsAsStrings(OriginalFunctionResult originalFunctionResult) {
        return originalFunctionResult.getCallExpressions().stream()
                .map(CallExpressionWithArgs::toString).collect(Collectors.toList());
    }

    public void test_FunctionType_ManyParams() {
        /*
         * Tests a series of partially applied functions that have many initial arguments.
         *
         * Only the first line in test data file 'print4<string, number, House>()<caret>;' tests the resolved
         * function for having the correct generic types and return type values as all other partial applications
         * reuse the same resolved function. It's all put in the 1 test as it reduces the number of test data needed
         * which duplicates a lot of code.
         */
        myFixture.configureByFile("FunctionType_ManyParams.ts");

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiElement elementAtCaret = myFixture.getFile().findElementAt(offset);
        VisualPosition currentVisualPosition = myFixture.getEditor().getCaretModel().getVisualPosition();

        // Get the original expression and resolved function so FunctionDeclarationResolver can be tested.
        JSExpressionStatement originalExpression = ResolveUtils.getExpression(elementAtCaret);
        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(originalExpression, JSReferenceExpression.class);

        assertThat(reference, is(notNullValue()));
        PsiElement resolvedFunction = reference.resolve();
        assertThat(resolvedFunction, is(notNullValue()));
        assertThat(resolvedFunction, instanceOf(TypeScriptFunction.class));

        FunctionDeclarationResolveResult resolveResult = FunctionDeclarationResolver
                .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);
        assertTrue(resolveResult.getTypeEvaluator().isPresent());

        assertTrue(resolveResult.getOriginalFunctionResult().isPresent());
        assertTrue(resolveResult.getResolvedFunctionResult().isPresent());

        // Test results of FunctionDeclarationResolver are as expected.
        OriginalFunctionResult originalFunctionResult = resolveResult.getOriginalFunctionResult().get();
        ResolvedFunctionResult resolvedFunctionResult = resolveResult.getResolvedFunctionResult().get();

        assertThat(callExpressionsAsStrings(originalFunctionResult), contains("(t1Value, 5)"));
        assertThat(originalFunctionResult.getActualGenericTypeValues(), contains("string", "number", "House"));
        assertThat(resolvedFunctionResult.getActualGenericTypeValues(), contains("T", "A", "C"));

        List<String> expectedReturnTypes = Arrays.asList(
                "(t1: Type1, t2: number) => (t: T, a: A) => (c: A) => (d: T) => (e: C) => string",
                "(t: T, a: A) => (c: A) => (d: T) => (e: C) => string",
                "(c: A) => (d: T) => (e: C) => string",
                "(d: T) => (e: C) => string",
                "(e: C) => string",
                "string"
        );

        assertThat(resolvedFunctionResult.getFunctionValue().getAllReturnTypes(), contains(expectedReturnTypes.toArray()));

        String returnType = resolveResult.getTypeEvaluator().get().evaluate();
        assertThat(returnType, is("(t: string, a: number) => (c: number) => (d: string) => (e: House) => string"));

        // ##### Sample 2 - print4<string, number, House>()("hi", "there");

        currentVisualPosition = new VisualPosition(currentVisualPosition.line + 1, 0);
        myFixture.getEditor().getCaretModel().moveToVisualPosition(currentVisualPosition);

        offset = myFixture.getEditor().getCaretModel().getOffset();
        elementAtCaret = myFixture.getFile().findElementAt(offset);

        // Get the original expression and resolved function so FunctionDeclarationResolver can be tested.
        originalExpression = ResolveUtils.getExpression(elementAtCaret);

        resolveResult = FunctionDeclarationResolver
                .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);
        assertTrue(resolveResult.getTypeEvaluator().isPresent());

        assertTrue(resolveResult.getOriginalFunctionResult().isPresent());

        originalFunctionResult = resolveResult.getOriginalFunctionResult().get();

        assertThat(callExpressionsAsStrings(originalFunctionResult), contains("(t1Value, 5)", "(\"hi\", \"there\")"));
        assertThat(originalFunctionResult.getActualGenericTypeValues(), contains("string", "number", "House"));

        returnType = resolveResult.getTypeEvaluator().get().evaluate();
        assertThat(returnType, is("(c: number) => (d: string) => (e: House) => string"));

        // ##### Sample 3 - print4<string, number, House>()("hi", "there")(4);

        currentVisualPosition = new VisualPosition(currentVisualPosition.line + 1, 0);
        myFixture.getEditor().getCaretModel().moveToVisualPosition(currentVisualPosition);

        offset = myFixture.getEditor().getCaretModel().getOffset();
        elementAtCaret = myFixture.getFile().findElementAt(offset);

        // Get the original expression and resolved function so FunctionDeclarationResolver can be tested.
        originalExpression = ResolveUtils.getExpression(elementAtCaret);

        resolveResult = FunctionDeclarationResolver
                .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);
        assertTrue(resolveResult.getTypeEvaluator().isPresent());

        assertTrue(resolveResult.getOriginalFunctionResult().isPresent());
        assertTrue(resolveResult.getResolvedFunctionResult().isPresent());

        // Test results of FunctionDeclarationResolver are as expected.
        originalFunctionResult = resolveResult.getOriginalFunctionResult().get();

        assertThat(callExpressionsAsStrings(originalFunctionResult), contains("(t1Value, 5)", "(\"hi\", \"there\")", "(4)"));
        assertThat(originalFunctionResult.getActualGenericTypeValues(), contains("string", "number", "House"));

        returnType = resolveResult.getTypeEvaluator().get().evaluate();
        assertThat(returnType, is("(d: string) => (e: House) => string"));

        // ##### Sample 4 - print4<string, number, House>()("hi", "there")(4)("world");

        currentVisualPosition = new VisualPosition(currentVisualPosition.line + 1, 0);
        myFixture.getEditor().getCaretModel().moveToVisualPosition(currentVisualPosition);

        offset = myFixture.getEditor().getCaretModel().getOffset();
        elementAtCaret = myFixture.getFile().findElementAt(offset);

        // Get the original expression and resolved function so FunctionDeclarationResolver can be tested.
        originalExpression = ResolveUtils.getExpression(elementAtCaret);

        resolveResult = FunctionDeclarationResolver
                .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);
        assertTrue(resolveResult.getTypeEvaluator().isPresent());

        assertTrue(resolveResult.getOriginalFunctionResult().isPresent());
        assertTrue(resolveResult.getResolvedFunctionResult().isPresent());

        // Test results of FunctionDeclarationResolver are as expected.
        originalFunctionResult = resolveResult.getOriginalFunctionResult().get();

        assertThat(callExpressionsAsStrings(originalFunctionResult), contains("(t1Value, 5)", "(\"hi\", \"there\")", "(4)", "(\"world\")"));
        assertThat(originalFunctionResult.getActualGenericTypeValues(), contains("string", "number", "House"));

        returnType = resolveResult.getTypeEvaluator().get().evaluate();
        assertThat(returnType, is("(e: House) => string"));

        // ##### Sample 5 - print4<string, number, House>()("hi", "there")(4)("world")(house);;

        currentVisualPosition = new VisualPosition(currentVisualPosition.line + 1, 0);
        myFixture.getEditor().getCaretModel().moveToVisualPosition(currentVisualPosition);

        offset = myFixture.getEditor().getCaretModel().getOffset();
        elementAtCaret = myFixture.getFile().findElementAt(offset);

        // Get the original expression and resolved function so FunctionDeclarationResolver can be tested.
        originalExpression = ResolveUtils.getExpression(elementAtCaret);

        resolveResult = FunctionDeclarationResolver
                .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);
        assertTrue(resolveResult.getTypeEvaluator().isPresent());

        assertTrue(resolveResult.getOriginalFunctionResult().isPresent());
        assertTrue(resolveResult.getResolvedFunctionResult().isPresent());

        // Test results of FunctionDeclarationResolver are as expected.
        originalFunctionResult = resolveResult.getOriginalFunctionResult().get();

        assertThat(callExpressionsAsStrings(originalFunctionResult), contains("(t1Value, 5)", "(\"hi\", \"there\")", "(4)", "(\"world\")", "(house)"));
        assertThat(originalFunctionResult.getActualGenericTypeValues(), contains("string", "number", "House"));

        returnType = resolveResult.getTypeEvaluator().get().evaluate();
        assertThat(returnType, is("string"));

        // ##### Sample 5 - print4<string, number, House>()("hi", "there")(4)("world")(house);
        // applying more function calls that exists returns the final return type.

        currentVisualPosition = new VisualPosition(currentVisualPosition.line + 1, 0);
        myFixture.getEditor().getCaretModel().moveToVisualPosition(currentVisualPosition);

        offset = myFixture.getEditor().getCaretModel().getOffset();
        elementAtCaret = myFixture.getFile().findElementAt(offset);

        // Get the original expression and resolved function so FunctionDeclarationResolver can be tested.
        originalExpression = ResolveUtils.getExpression(elementAtCaret);

        resolveResult = FunctionDeclarationResolver
                .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);
        assertTrue(resolveResult.getTypeEvaluator().isPresent());

        assertTrue(resolveResult.getOriginalFunctionResult().isPresent());
        assertTrue(resolveResult.getResolvedFunctionResult().isPresent());

        // Test results of FunctionDeclarationResolver are as expected.
        originalFunctionResult = resolveResult.getOriginalFunctionResult().get();

        assertThat(callExpressionsAsStrings(originalFunctionResult), contains("(t1Value, 5)", "(\"hi\", \"there\")", "(4)", "(\"world\")", "(house)", "(\"TooManyArgs\")"));
        assertThat(originalFunctionResult.getActualGenericTypeValues(), contains("string", "number", "House"));

        returnType = resolveResult.getTypeEvaluator().get().evaluate();
        assertThat(returnType, is("string"));
    }

    public void test_FunctionType_ZeroParams() {
        /*
         * Tests a series of partially applied functions that have zero initial arguments.
         *
         * Its the same test as test_FunctionType_ManyParams, but tests to ensure an empty call () is accounted
         * for in the return types. This is vital because if its missing the incorrect return type will be returned
         * since the number of call expressions determines which index to retrieve in the return types list.
         */
        myFixture.configureByFile("FunctionType_ZeroParams.ts");

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiElement elementAtCaret = myFixture.getFile().findElementAt(offset);

        // Get the original expression and resolved function so FunctionDeclarationResolver can be tested.
        JSExpressionStatement originalExpression = ResolveUtils.getExpression(elementAtCaret);
        JSReferenceExpression reference = PsiTreeUtil.findChildOfType(originalExpression, JSReferenceExpression.class);

        assertThat(reference, is(notNullValue()));
        PsiElement resolvedFunction = reference.resolve();
        assertThat(resolvedFunction, is(notNullValue()));
        assertThat(resolvedFunction, instanceOf(TypeScriptFunction.class));

        FunctionDeclarationResolveResult resolveResult = FunctionDeclarationResolver
                .functionDeclarationTypeEvaluator(originalExpression, resolvedFunction);
        assertTrue(resolveResult.getTypeEvaluator().isPresent());

        assertTrue(resolveResult.getOriginalFunctionResult().isPresent());
        assertTrue(resolveResult.getResolvedFunctionResult().isPresent());

        // Test results of FunctionDeclarationResolver are as expected.
        OriginalFunctionResult originalFunctionResult = resolveResult.getOriginalFunctionResult().get();
        ResolvedFunctionResult resolvedFunctionResult = resolveResult.getResolvedFunctionResult().get();

        assertThat(callExpressionsAsStrings(originalFunctionResult), contains("()", "(\"hi\", \"there\")", "(4)", "(\"world\")"));
        assertThat(originalFunctionResult.getActualGenericTypeValues(), contains("string", "number", "House"));
        assertThat(resolvedFunctionResult.getActualGenericTypeValues(), contains("T", "A", "C"));

        List<String> expectedReturnTypes = Arrays.asList(
                "() => (t: T, a: A) => (c: A) => (d: T) => (e: C) => string",
                "(t: T, a: A) => (c: A) => (d: T) => (e: C) => string",
                "(c: A) => (d: T) => (e: C) => string",
                "(d: T) => (e: C) => string",
                "(e: C) => string",
                "string"
        );

        assertThat(resolvedFunctionResult.getFunctionValue().getAllReturnTypes(), contains(expectedReturnTypes.toArray()));

        String returnType = resolveResult.getTypeEvaluator().get().evaluate();
        assertThat(returnType, is("(e: House) => string"));

    }
}