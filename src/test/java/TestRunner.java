import variableAssignment.LightAssignTypeToVariableIntentionTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Created by matt on 01-Jul-17.
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        LightTypeAssistDocumentationProviderTest.class,
        LightAssignTypeToVariableIntentionTest.class,
        HtmlUtilsTest.class
})
public class TestRunner {
}
