package documentation.textReplacement;

/**
 * Instructions for finding and replacing a value within the documentation string.
 *
 * <p>Do not remove equals and hashCode, as the {@code originalKey} is used to determine distinct values.</p>
 */
public class FindReplaceValue {
    private String originalKey;
    private String regexSearchKey;
    private String replacementValue;

    private FindReplaceValue(String originalKey, String regexSearchKey, String replacementValue) {
        this.originalKey = originalKey;
        this.regexSearchKey = regexSearchKey;
        this.replacementValue = replacementValue;
    }

    public static FindReplaceValue of(String originalKey, String regexSearchKey, String replacementValue) {
        return new FindReplaceValue(originalKey, regexSearchKey, replacementValue);
    }

    public static String wordBoundary(String value) {
        return "\\b" + value + "\\b";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FindReplaceValue that = (FindReplaceValue) o;

        return originalKey.equals(that.originalKey);
    }

    @Override
    public int hashCode() {
        return originalKey.hashCode();
    }

    @Override
    public String toString() {
        return "FindReplaceValue{" +
                "originalKey='" + originalKey + '\'' +
                ", regexSearchKey='" + regexSearchKey + '\'' +
                ", replacementValue='" + replacementValue + '\'' +
                '}';
    }

    public String getOriginalKey() {
        return originalKey;
    }

    public String getRegexSearchKey() {
        return regexSearchKey;
    }

    public String getReplacementValue() {
        return replacementValue;
    }
}