package documentation.textReplacement;

import com.intellij.codeInsight.documentation.DocumentationManagerUtil;

import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Given a list of resolvable types such as [Person, Address], map them into a list of {@code FindReplaceValue}s
 * containing the original value, a regex lookup key and replacement value, such that each property within an
 * object type can be searched and replaced.
 *
 * <p>
 * {@code FindReplaceValue(Person, \\bPerson\\b, hyperlink with Person)}<br>
 * {@code FindReplaceValue(Address, \\bAddress\\b, hyperlink with Address)}
 * <p>
 *
 * The supplied {@code typeName} is to avoid creating hyperlinks to self for recursive types. For example, the
 * {@code typeName} here is {@code Tree<T>}. For any resolvable type, if the start of the {@code FindReplaceValue.regexSearchKey}
 * is found at the beginning of the {@code typeName}, it is excluded from the list of eligible resolvable types
 * to be replaced with a hyperlink.
 *
 * <pre>
 *      interface{@code Tree<T>} {
 *         value: T
 *         left:{@code Tree<T>}
 *         right:{@code Tree<T>}
 *     }
 * </pre>
 */
public class FindReplaceResolvableReference {
    private List<FindReplaceValue> findReplaceValues;

    private FindReplaceResolvableReference(List<FindReplaceValue> findReplaceValues) {
        this.findReplaceValues = findReplaceValues;
    }

    /**
     * Accepts the list of resolvable reference types such as
     *
     * <p>{@code FindReplaceValue{originalKey='Status', regexSearchKey='\bStatus\b', replacementValue='Status'}}</p>
     *
     * And transforms the replacement value into a hyperlink
     *
     * <p>{@code FindReplaceValue{originalKey='Status', regexSearchKey='\bStatus\b', replacementValue='Status hyperlink'}}</p>
     *
     * @param typeName The main type name of the interface for example such as Tree in the main class docs.
     */
    public static FindReplaceResolvableReference of(List<FindReplaceValue> resolvableReferenceTypes,
                                                    String typeName) {
        /*
         * Accepts the resolved type and turns it into a hyperlink.
         * The lookup key must be on a word boundary otherwise sub words could get accidentally get replaced.
         */
        Function<FindReplaceValue, FindReplaceValue> toReplacementPair = resolvedType -> {
            StringBuilder sb = new StringBuilder();
            DocumentationManagerUtil.createHyperlink(sb, resolvedType.getReplacementValue(), resolvedType.getOriginalKey(), false);
            return FindReplaceValue.of(resolvedType.getOriginalKey(), resolvedType.getRegexSearchKey(), sb.toString());
        };

        // Handle recursive types to avoid hyperlinks to self.
        Predicate<FindReplaceValue> excludeRefsWithSameTypeName =
                value -> !Pattern.compile("^" + value.getRegexSearchKey()).matcher(typeName).find();

        List<FindReplaceValue> replacements = resolvableReferenceTypes.stream()
                .map(toReplacementPair)
                .filter(excludeRefsWithSameTypeName)
                .collect(Collectors.toList());

        return new FindReplaceResolvableReference(replacements);
    }

    public List<FindReplaceValue> getFindReplaceValues() {
        return findReplaceValues;
    }
}
