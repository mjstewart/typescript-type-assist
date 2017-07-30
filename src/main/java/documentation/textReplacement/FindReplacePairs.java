package documentation.textReplacement;

import java.util.ArrayList;
import java.util.List;

/**
 * Aggregates all find and replaceable pairs into a single list. {@code toDocumentationType} will iterate
 * through this list trying to find/replace.
 */
public class FindReplacePairs {
    private List<FindReplaceValue> replacementPairs;

    private FindReplacePairs(FindReplaceResolvableReference resolvableReference,
                             FindReplaceGenericTypeParameter genericTypeParameter) {
        replacementPairs = new ArrayList<>();
        replacementPairs.addAll(resolvableReference.getFindReplaceValues());
        replacementPairs.addAll(genericTypeParameter.getFindReplaceValues());
    }

    public static FindReplacePairs of(FindReplaceResolvableReference resolvableReference,
                                      FindReplaceGenericTypeParameter genericTypeParameter) {
        return new FindReplacePairs(resolvableReference, genericTypeParameter);
    }

    public List<FindReplaceValue> getReplacementPairs() {
        return replacementPairs;
    }
}
