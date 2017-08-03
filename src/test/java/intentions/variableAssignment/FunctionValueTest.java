package intentions.variableAssignment;

import intentions.variableAssignment.functions.FunctionType;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * This test only requires testing FunctionValue.getAllReturnTypes, so any instance of FunctionType can be used purely
 * to get access to this method.
 */
public class FunctionValueTest {

    @Test
    public void allReturnTypes_Many() {
        List<String> expected = Arrays.asList(
                "(t: T, a: A) => (c: A) => (d: T) => string",
                "(c: A) => (d: T) => string",
                "(d: T) => string",
                "string"
        );

        String signature = "(t: T, a: A) => (c: A) => (d: T) => string";
        FunctionType functionType = new FunctionType(signature, Collections.emptyList());

        List<String> allReturnTypes = functionType.getAllReturnTypes(signature);

        assertThat(allReturnTypes, hasSize(4));
        assertThat(allReturnTypes, containsInAnyOrder(expected.toArray()));
    }

    @Test
    public void allReturnTypes_Single() {
        String signature = "number";
        FunctionType functionType = new FunctionType(signature, Collections.emptyList());
        List<String> allReturnTypes = functionType.getAllReturnTypes(signature);

        assertThat(allReturnTypes, hasSize(1));
        assertThat(signature, is(allReturnTypes.get(0)));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void allReturnTypes_null() {
        String signature = null;
        FunctionType functionType = new FunctionType(signature, Collections.emptyList());
        List<String> allReturnTypes = functionType.getAllReturnTypes(signature);

        assertThat(allReturnTypes, hasSize(0));
    }
}