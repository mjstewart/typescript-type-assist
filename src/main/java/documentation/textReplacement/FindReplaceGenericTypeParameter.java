package documentation.textReplacement;

import settings.TypeAssistApplicationSettings;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FindReplaceGenericTypeParameter {
    private List<FindReplaceValue> findReplaceValues;

    private FindReplaceGenericTypeParameter(List<FindReplaceValue> findReplaceValues) {
        this.findReplaceValues = findReplaceValues;
    }

    /**
     * For all type parameters in the supplied {@code genericTypeParameters}, map into a value object containing
     * the instructions to execute the find/replace.
     *
     * <p>Eg if the List of generic type parameter Strings is [X, Y] then the following elements are created.</p>
     *
     * <pre>
     *     FindReplaceValue(X, \bX\b, <span style="color:#41B8E7">X</span>)
     *     FindReplaceValue(Y, \bY\b, <span style="color:#41B8E7">Y</span>)
     * </pre>
     */
    public static FindReplaceGenericTypeParameter of(List<String> genericTypeParameters) {
        TypeAssistApplicationSettings settings = TypeAssistApplicationSettings.getInstance();

        // The lookup key must be on a word boundary otherwise sub words could get accidentally get replaced.
        Function<String, FindReplaceValue> toReplacementPair = genericType ->
                FindReplaceValue.of(genericType, FindReplaceValue.wordBoundary(genericType), HtmlUtils.span(genericType, settings.GENERICS_HEX_COLOR));

        List<FindReplaceValue> replacements = genericTypeParameters.stream()
                .map(toReplacementPair).collect(Collectors.toList());

        return new FindReplaceGenericTypeParameter(replacements);
    }

    public List<FindReplaceValue> getFindReplaceValues() {
        return findReplaceValues;
    }
}
