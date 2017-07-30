package variableAssignment;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.lang.javascript.psi.JSVarStatement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * These tests are for testing the logic used to extract the type information from a standard function eg,
 * a function declared with a {@code function} keyword.
 *
 * <p>The actual intention is not tested, instead test data is used to rebuild Typescript PsiElements that would be passed
 * into the real intention. These PsiElements are used to test {@code FunctionDeclarationResolver} which is the
 * root entry point that determines the final type that the intention simply just writes to the document.</p>
 *
 * <p>These tests do not go into detail regarding missing PsiElement in the test data as its assumed its all valid.
 * If there are issues then it has to do with the test data, not the tested code.</p>
 */
@SuppressWarnings("ConstantConditions")
public class LightAssignTypeToVariableIntentionTest extends LightCodeInsightFixtureTestCase {

    // Todo: Test will fail since intention isn't registered as this feature isn't in use.

    @Override
    protected String getTestDataPath() {
        return "src/test/java/variableAssignment/testData";
    }

    public void test_AssignedVariableType() {
        myFixture.configureByFile("AssignedVariableType.ts");

        List<IntentionAction> intention = myFixture.filterAvailableIntentions("Add type to variable");
        myFixture.launchAction(intention.get(0));

        int lineStart = myFixture.getEditor().getCaretModel().getVisualLineStart();

        JSVarStatement varStatement = PsiTreeUtil.getTopmostParentOfType(myFixture.getFile().findElementAt(lineStart), JSVarStatement.class);
        String expected = "const booking: number | State1<Config, House, boolean> | boolean | House<string & boolean> | State2<House & boolean>[] = booking<string, Config, House, boolean>(5);";
        assertThat(varStatement.getText(), is(expected));
    }
}