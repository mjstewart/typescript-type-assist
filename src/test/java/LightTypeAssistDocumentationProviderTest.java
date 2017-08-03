import com.intellij.codeInsight.documentation.DocumentationManagerUtil;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import documentation.HtmlUtils;
import documentation.TypeAssistDocumentationProvider;
import settings.TypeAssistApplicationSettings;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * {@code TypeAssistDocumentationProvider} depends on {@code DocumentationManagerUtil) and
 * {@code TypeAssistApplicationSettings} existing. It appears test needs to be prepended for the tests to run.
 */
public class LightTypeAssistDocumentationProviderTest extends LightCodeInsightFixtureTestCase {

    private TypeAssistDocumentationProvider provider;
    private TypeAssistApplicationSettings settings;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        provider = new TypeAssistDocumentationProvider();
        settings = TypeAssistApplicationSettings.getInstance();
    }

    public void test_toTypeName_NoGenericParameters_IsNotReplaced() {
        /*
         * typeName contains no generic parameters and is resolvable. Person should get replaced with a hyperlink as
         * it makes no sense to recursively link back to self.
         *
         * No white space is tested as toTypeName is provided a perfectly formed string built by TypeDescription.
         */
        TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter genericTypeParameter =
                TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter.of(Collections.emptyList());

        String typeName = "Person";
        TypeAssistDocumentationProvider.FindReplaceResolvableReference resolvableReference =
                TypeAssistDocumentationProvider.FindReplaceResolvableReference.of(Arrays.asList(typeName), typeName);

        TypeAssistDocumentationProvider.FindReplacePairs findReplacePairs =
                TypeAssistDocumentationProvider.FindReplacePairs.of(resolvableReference, genericTypeParameter);

        assertThat(provider.toTypeName("Person", findReplacePairs), is("Person"));
    }

    public void test_toTypeName_SingleGenericParameters_IsReplaced() {
        /*
         * There is only 1 generic type T which gets replaced. Since Person<T> is the type name and Person was found
         * to be resolvable, it should not be replaced with a hyperlink as it makes no sense to recursively link back to self.
         *
         * No white space is tested as toTypeName is provided a perfectly formed string built by TypeDescription.
         */
        TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter genericTypeParameter =
                TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter.of(Arrays.asList("T"));

        String typeName = "Person<T>";
        TypeAssistDocumentationProvider.FindReplaceResolvableReference resolvableReference =
                TypeAssistDocumentationProvider.FindReplaceResolvableReference.of(Arrays.asList("Person"), typeName);

        TypeAssistDocumentationProvider.FindReplacePairs findReplacePairs =
                TypeAssistDocumentationProvider.FindReplacePairs.of(resolvableReference, genericTypeParameter);

        String expect = String.format("Person<%s>", HtmlUtils.span("T", settings.GENERICS_HEX_COLOR));
        assertThat(provider.toTypeName("Person<T>", findReplacePairs), is(expect));
    }

    public void test_toTypeName_ManyGenericParameters_AllReplaced() {
        /*
         * There are many generic types which all get replaced with colored spans. Person is resolvable so should not
         * get replaced with a hyperlink to prevent recursively linking back to self.
         * However since Product is a resolvable type it will get replaced with a hyperlink.
         *
         * No white space is tested as toTypeName is provided a perfectly formed string built by TypeDescription.
         */
        TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter genericTypeParameter =
                TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter.of(Arrays.asList("T", "X", "Y", "Z"));

        String typeName =  "Person<T extends Product, X extends number, Y, Z>";
        TypeAssistDocumentationProvider.FindReplaceResolvableReference resolvableReference =
                TypeAssistDocumentationProvider.FindReplaceResolvableReference.of(Arrays.asList("Person", "Product"), typeName);

        TypeAssistDocumentationProvider.FindReplacePairs findReplacePairs =
                TypeAssistDocumentationProvider.FindReplacePairs.of(resolvableReference, genericTypeParameter);

        // Product is the resolved type that will receive a hyperlink as it is not part of the type name Person.
        StringBuilder sb = new StringBuilder();
        DocumentationManagerUtil.createHyperlink(sb, "Product", "Product", false);
        String productHyperlink = sb.toString();

        String expect = String.format("Person<%s extends %s, %s extends number, %s, %s>",
                HtmlUtils.span("T", settings.GENERICS_HEX_COLOR),
                productHyperlink,
                HtmlUtils.span("X", settings.GENERICS_HEX_COLOR),
                HtmlUtils.span("Y", settings.GENERICS_HEX_COLOR),
                HtmlUtils.span("Z", settings.GENERICS_HEX_COLOR));

        assertThat(provider.toTypeName(typeName, findReplacePairs), is(expect));
    }

    public void test_toTypeName_ManyGenericParametersWithSameStartName_AllReplaced() {
        /*
         * There are many generic types all beginning with the same starting characters as the type name.
         *
         * This test is to ensure Tree which is the main type name does not get replaced with any colored spans for
         * any of the generic types.
         *
         * Since Tree is the main type name and is resolvable, it should not be replaced with a hyperlink as it
         * makes no sense to recursively link back to self.
         *
         * No white space is tested as toTypeName is provided a perfectly formed string built by TypeDescription.
         */
        String typeName =  "Tree<T, Tr, Tre>";

        TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter genericTypeParameter =
                TypeAssistDocumentationProvider.FindReplaceGenericTypeParameter.of(Arrays.asList("T", "Tr", "Tre"));

        TypeAssistDocumentationProvider.FindReplaceResolvableReference resolvableReference =
                TypeAssistDocumentationProvider.FindReplaceResolvableReference.of(Arrays.asList("Tree"), typeName);

        TypeAssistDocumentationProvider.FindReplacePairs findReplacePairs =
                TypeAssistDocumentationProvider.FindReplacePairs.of(resolvableReference, genericTypeParameter);

        String expect = String.format("Tree<%s, %s, %s>",
                HtmlUtils.span("T", settings.GENERICS_HEX_COLOR),
                HtmlUtils.span("Tr", settings.GENERICS_HEX_COLOR),
                HtmlUtils.span("Tre", settings.GENERICS_HEX_COLOR));

        assertThat(provider.toTypeName(typeName, findReplacePairs), is(expect));
    }

    public void test_replaceReadOnly_SingleReadOnlyExists_IsReplaced() {
        String expected1 = String.format("%s title: string", HtmlUtils.span(HtmlUtils.code("readonly"), settings.READONLY_HEX_COLOR));
        assertThat(provider.replaceReadOnly("readonly title: string"), is(expected1));

        // Allow for different spacing
        String expected2 = String.format("  %s          title :    string", HtmlUtils.span(HtmlUtils.code("readonly"), settings.READONLY_HEX_COLOR));
        assertThat(provider.replaceReadOnly("  readonly          title :    string"), is(expected2));
    }

    public void test_replaceReadOnly_ManyReadOnlyExists_AllReplaced() {
        String readonlySpan = HtmlUtils.span(HtmlUtils.code("readonly"), settings.READONLY_HEX_COLOR);
        String expected1 = String.format("{ success?: true; %s value: T } | { %s success: false; %s error: string };",
                readonlySpan, readonlySpan, readonlySpan);
        String actual1 = provider.replaceReadOnly("{ success?: true; readonly value: T } | { readonly success: false; readonly error: string };");
        assertThat(actual1, is(expected1));

        // Allow for different spacing
        String expected2 = String.format("{ success?:         true;   %s    value:   T }    | {    %s   success:   false;   %s      error:      string };",
                readonlySpan, readonlySpan, readonlySpan);
        String value = "{ success?:         true;   readonly    value:   T }    | {    readonly   success:   false;   readonly      error:      string };";
        String actual2 = provider.replaceReadOnly(value);
        assertThat(actual2, is(expected2));
    }

    public void test_replaceReadOnly_ReadOnlyDoesNotExist_IsNotReplaced() {
        String property = "title: string";
        String newValue = provider.replaceReadOnly(property);
        assertThat(newValue, is(property));
    }

    public void test_replaceOptionals_PropertySignatureWithOptional_IsReplaced() {
        assertThat(provider.replaceOptionals("title?: string"),
                is(HtmlUtils.span("title?", settings.OPTIONAL_HEX_COLOR) + ": string"));

        // Allow for different spacing
        String expected = String.format("  %s  :  string  ",
                HtmlUtils.span("title   ?", settings.OPTIONAL_HEX_COLOR));
        assertThat(provider.replaceOptionals("  title   ?  :  string  "), is(expected));
    }

    public void test_replaceOptionals_PropertySignatureNoOptional_IsNotReplaced() {
        String type = "title: string";
        assertThat(provider.replaceOptionals(type), is(type));
    }

    public void test_replaceOptionals_FunctionSignatureNoOptionalParameter_IsNotReplaced() {
        String functionType1 = "toString(title: string, firstName: string, age: number): string";
        String functionType2 = "(title: string, firstName: string, age: number)";

        // Allow for different spacing.
        String functionType3 = "toString ( title : string, firstName : string, age : number) : string";
        String functionType4 = "( title : string, firstName : string, age : number )";

        assertThat(provider.replaceOptionals(functionType1), is(functionType1));
        assertThat(provider.replaceOptionals(functionType2), is(functionType2));
        assertThat(provider.replaceOptionals(functionType3), is(functionType3));
        assertThat(provider.replaceOptionals(functionType4), is(functionType4));
    }

    public void test_replaceOptionals_FunctionSignatureSingleOptionalParameter_IsReplaced() {
        String functionType1 = "toString(title: string, firstName?: string, age: number): string";
        String expected1 = String.format("toString(title: string, %s: string, age: number): string",
                HtmlUtils.span("firstName?", settings.OPTIONAL_HEX_COLOR));

        // Allow for different spacing.
        String functionType2 = "toString  (  title :  string, firstName  ? : string, age : number): string";
        String expected2 = String.format("toString  (  title :  string, %s : string, age : number): string",
                HtmlUtils.span("firstName  ?", settings.OPTIONAL_HEX_COLOR));

        assertThat(provider.replaceOptionals(functionType1), is(expected1));
        assertThat(provider.replaceOptionals(functionType2), is(expected2));
    }

    public void test_replaceOptionals_FunctionSignatureManyOptionalParameters_IsReplaced() {
        String functionType1 = "toString(title?: string, firstName?: string, age?: number): string";
        String expected1 = String.format("toString(%s: string, %s: string, %s: number): string",
                HtmlUtils.span("title?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("firstName?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("age?", settings.OPTIONAL_HEX_COLOR));

        // Allow for different spacing.
        String functionType2 = "toString  (  title    ?: string ,  firstName        ? : string, age ?    : number): string";
        String expected2 = String.format("toString  (  %s: string ,  %s : string, %s    : number): string",
                HtmlUtils.span("title    ?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("firstName        ?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("age ?", settings.OPTIONAL_HEX_COLOR));

        assertThat(provider.replaceOptionals(functionType1), is(expected1));
        assertThat(provider.replaceOptionals(functionType2), is(expected2));
    }

    public void test_replaceOptionals_FunctionSignatureWithOptionalReturn_IsReplaced() {
        // Eventually TypeScript could have optional return type: string? vs string | undefined.
        String functionType1 = "functionName(title: string, firstName: string, age: number): string? | Person?";
        String expected1 = String.format("functionName(title: string, firstName: string, age: number): %s | %s",
                HtmlUtils.span("string?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("Person?", settings.OPTIONAL_HEX_COLOR));

        // Allow for different spacing.
        String functionType2 = "functionName     (title: string,   firstName: string,   age:    number):   string? |        Person      ?";
        String expected2 = String.format("functionName     (title: string,   firstName: string,   age:    number):   %s |        %s",
                HtmlUtils.span("string?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("Person      ?", settings.OPTIONAL_HEX_COLOR));

        assertThat(provider.replaceOptionals(functionType1), is(expected1));
        assertThat(provider.replaceOptionals(functionType2), is(expected2));
    }

    public void test_replaceOptionals_FunctionSignaturePredicateWithOptionalReturn_OnlyOptionalReturnIsReplaced() {
        // Eventually TypeScript could have optional return type: string? vs string | undefined.
        String functionType1 = "functionName?(title: string, firstName: string, age?: number): string? | Person?";
        String expected1 = String.format("functionName?(title: string, firstName: string, %s: number): %s | %s",
                HtmlUtils.span("age?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("string?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("Person?", settings.OPTIONAL_HEX_COLOR));

        // Allow for different spacing.
        String functionType2 = "functionName  ?   (title: string,   firstName: string,   age?:    number):   string? |        Person      ?";
        String expected2 = String.format("functionName  ?   (title: string,   firstName: string,   %s:    number):   %s |        %s",
                HtmlUtils.span("age?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("string?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("Person      ?", settings.OPTIONAL_HEX_COLOR));

        assertThat(provider.replaceOptionals(functionType1), is(expected1));
        assertThat(provider.replaceOptionals(functionType2), is(expected2));
    }

    public void test_replaceOptionals_FunctionSignaturePredicateWithNoOptionals_IsNotReplaced() {
        String functionType1 = "personExists?(firstName: string, age: number): boolean";
        assertThat(provider.replaceOptionals(functionType1), is(functionType1));

        // Allow for different spacing.
        String functionType2 = "personExists   ?   (    firstName  : string,   age  :   number)    :   boolean";
        assertThat(provider.replaceOptionals(functionType2), is(functionType2));
    }

    public void test_replaceOptionals_FunctionSignaturePredicateWithOptionalParameters_IsReplaced() {
        String functionType1 = "personExists?(firstName: string, age?: number): boolean";
        String expected1 = String.format("personExists?(firstName: string, %s: number): boolean",
                HtmlUtils.span("age?", settings.OPTIONAL_HEX_COLOR));

        String functionType2 = "  personExists  ?   (firstName  :   string,   age  ?  : number   )    : boolean   ";
        String expected2 = String.format("  personExists  ?   (firstName  :   string,   %s  : number   )    : boolean   ",
                HtmlUtils.span("age  ?", settings.OPTIONAL_HEX_COLOR));

        assertThat(provider.replaceOptionals(functionType1), is(expected1));
        assertThat(provider.replaceOptionals(functionType2), is(expected2));
    }

    public void test_replaceOptionals_NotOptionalAndQuestionMarkIsWithinWord_IsNotReplaced() {
        // Its not an Optional type if there is a question mark anywhere except at the end of a variable.
        String functionType1 = "?personExi?sts(fir?stName: string, a?ge: nu?mber): ?bo?olean";
        String functionType2 = "?personExi?sts(fir?stName  :      string,     ag?e: nu?mber)     :       ?bo?olean";

        assertThat(provider.replaceOptionals(functionType1), is(functionType1));
        assertThat(provider.replaceOptionals(functionType2), is(functionType2));
    }

    public void test_replaceOptionals_IsOptionalAndQuestionMarkIsWithinWord_IsReplaced() {
        // Its an Optional type but has another question mark in the word which is not good code style but it might exist.
        String functionType1 = "?personExi?sts(fir?stName?: string, a?ge?: nu?mber): ?bo?olean";
        String expected1 = String.format("?personExi?sts(%s: string, %s: nu?mber): ?bo?olean",
                HtmlUtils.span("fir?stName?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("a?ge?", settings.OPTIONAL_HEX_COLOR));

        String functionType2 = "    ?personExi?sts   (            fir?stName?          : string,    a?ge?:      nu?mber)   :   ?bo?olean";
        String expected2 = String.format("    ?personExi?sts   (            %s          : string,    %s:      nu?mber)   :   ?bo?olean",
                HtmlUtils.span("fir?stName?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("a?ge?", settings.OPTIONAL_HEX_COLOR));

        assertThat(provider.replaceOptionals(functionType1), is(expected1));
        assertThat(provider.replaceOptionals(functionType2), is(expected2));
    }

    public void test_replaceOptionals_SingleCharacterOptional_IsReplaced() {
        // Its not an Optional type if there is a question mark anywhere except at the end of a variable.
        String functionType1 = "toString(a: string, b?: string, c?: number): string";
        String expected = String.format("toString(a: string, %s: string, %s: number): string",
                HtmlUtils.span("b?", settings.OPTIONAL_HEX_COLOR),
                HtmlUtils.span("c?", settings.OPTIONAL_HEX_COLOR));

        assertThat(provider.replaceOptionals(functionType1), is(expected));
    }

    public void test_replaceOptionals_MappedTypeNoOptional_IsNotReplaced() {
        // Mapped type is not an optional
        String mappedType1 = "[P in keyof Person]";
        String mappedType2 = "         [P   in     keyof     Person   ]       ";

        assertThat(provider.replaceOptionals(mappedType1), is(mappedType1));
        assertThat(provider.replaceOptionals(mappedType2), is(mappedType2));
    }

    public void test_replaceOptionals_MappedTypeOptional_IsReplaced() {
        // Mapped type is an optional, the whole expression is wrapped in an optional colored span. If Person was
        // a hyperlink it would get wrapped in the optional span color too removing any natural href styles of blue.

        String mappedType1 = "[P in keyof Person]?";
        String expected1 = HtmlUtils.span("[P in keyof Person]?", settings.OPTIONAL_HEX_COLOR);

        String mappedType2 = "         [P   in     keyof     Person   ]      ?       ";
        String expected2 = HtmlUtils.span("         [P   in     keyof     Person   ]      ?       ", settings.OPTIONAL_HEX_COLOR);

        assertThat(provider.replaceOptionals(mappedType1), is(expected1));
        assertThat(provider.replaceOptionals(mappedType2), is(expected2));
    }

    public void test_replaceUnspecifiedTypes_NoUnspecifiedTypes_IsNotReplaced() {
        // When there is no unspecified types, nothing should get replaced.
        String functionType = "someFunction(value: string, padding: string | number): string | number";
        assertThat(provider.replaceUnspecifiedTypes(functionType), is(functionType));
    }

    public void test_replaceUnspecifiedTypes_UnspecifiedTypes_NullIsReplaced() {
        // When the unspecified type is null, only null should get replaced.
        String functionType1 = "someFunction(value: null, padding: string | null): string | number";
        String expected1 = String.format("someFunction(value: %s, padding: string | %s): string | number",
                HtmlUtils.span("null", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("null", settings.UNDEFINED_HEX_COLOR));

        String functionType2 = "someFunction    (   value  :      null, padding   : string       |              null): string                   | number";
        String expected2 = String.format("someFunction    (   value  :      %s, padding   : string       |              %s): string                   | number",
                HtmlUtils.span("null", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("null", settings.UNDEFINED_HEX_COLOR));

        assertThat(provider.replaceUnspecifiedTypes(functionType1), is(expected1));
        assertThat(provider.replaceUnspecifiedTypes(functionType2), is(expected2));
    }

    public void test_replaceUnspecifiedTypes_UnspecifiedTypes_UndefinedIsReplaced() {
        // When the unspecified type is null, only null should get replaced.
        String functionType1 = "someFunction(value: undefined, padding: string | undefined): string | number";
        String expected1 = String.format("someFunction(value: %s, padding: string | %s): string | number",
                HtmlUtils.span("undefined", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("undefined", settings.UNDEFINED_HEX_COLOR));

        String functionType2 = "someFunction    (   value  :      undefined, padding   : string       |              undefined): string                   | number";
        String expected2 = String.format("someFunction    (   value  :      %s, padding   : string       |              %s): string                   | number",
                HtmlUtils.span("undefined", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("undefined", settings.UNDEFINED_HEX_COLOR));

        assertThat(provider.replaceUnspecifiedTypes(functionType1), is(expected1));
        assertThat(provider.replaceUnspecifiedTypes(functionType2), is(expected2));
    }

    public void test_replaceUnspecifiedTypes_UnspecifiedTypes_AnyIsReplaced() {
        // When the unspecified type is null, only null should get replaced.
        String functionType1 = "someFunction(value: any, padding: string | any): string | number";
        String expected1 = String.format("someFunction(value: %s, padding: string | %s): string | number",
                HtmlUtils.span("any", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("any", settings.UNDEFINED_HEX_COLOR));

        String functionType2 = "someFunction    (   value  :      any, padding   : string       |              any): string                   | number";
        String expected2 = String.format("someFunction    (   value  :      %s, padding   : string       |              %s): string                   | number",
                HtmlUtils.span("any", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("any", settings.UNDEFINED_HEX_COLOR));

        assertThat(provider.replaceUnspecifiedTypes(functionType1), is(expected1));
        assertThat(provider.replaceUnspecifiedTypes(functionType2), is(expected2));
    }

    public void test_replaceUnspecifiedTypes_UnspecifiedTypes_VoidIsReplaced() {
        // When the unspecified type is null, only null should get replaced.
        String functionType1 = "someFunction(value: string, padding: string | number): void";
        String expected1 = String.format("someFunction(value: string, padding: string | number): %s",
                HtmlUtils.span("void", settings.UNDEFINED_HEX_COLOR));

        String functionType2 = "        someFunction(            value: string, padding:        string     |   number)     :            void     ";
        String expected2 = String.format("        someFunction(            value: string, padding:        string     |   number)     :            %s     ",
                HtmlUtils.span("void", settings.UNDEFINED_HEX_COLOR));

        assertThat(provider.replaceUnspecifiedTypes(functionType1), is(expected1));
        assertThat(provider.replaceUnspecifiedTypes(functionType2), is(expected2));
    }

    public void test_replaceUnspecifiedTypes_UnspecifiedTypes_NeverIsReplaced() {
        // When the unspecified type is null, only null should get replaced.
        String functionType1 = "someFunction(value: string, padding: string | number): never";
        String expected1 = String.format("someFunction(value: string, padding: string | number): %s",
                HtmlUtils.span("never", settings.UNDEFINED_HEX_COLOR));

        String functionType2 = "        someFunction(            value: string, padding:        string     |   number)     :            never     ";
        String expected2 = String.format("        someFunction(            value: string, padding:        string     |   number)     :            %s     ",
                HtmlUtils.span("never", settings.UNDEFINED_HEX_COLOR));

        assertThat(provider.replaceUnspecifiedTypes(functionType1), is(expected1));
        assertThat(provider.replaceUnspecifiedTypes(functionType2), is(expected2));
    }

    public void test_replaceUnspecifiedTypes_UnspecifiedTypes_AllTypesReplaced() {
        // When the unspecified type is null, only null should get replaced.
        String functionType1 = "someFunction(value: string, age: null, person: undefined, padding: string | any): never | void";
        String expected1 = String.format("someFunction(value: string, age: %s, person: %s, padding: string | %s): %s | %s",
                HtmlUtils.span("null", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("undefined", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("any", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("never", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("void", settings.UNDEFINED_HEX_COLOR));

        String functionType2 = "    someFunction       (value: string,    age   :  null, person :    undefined, padding: string |              any)  :   never   |         void            ";
        String expected2 = String.format("    someFunction       (value: string,    age   :  %s, person :    %s, padding: string |              %s)  :   %s   |         %s            ",
                HtmlUtils.span("null", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("undefined", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("any", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("never", settings.UNDEFINED_HEX_COLOR),
                HtmlUtils.span("void", settings.UNDEFINED_HEX_COLOR));

        assertThat(provider.replaceUnspecifiedTypes(functionType1), is(expected1));
        assertThat(provider.replaceUnspecifiedTypes(functionType2), is(expected2));
    }
}
