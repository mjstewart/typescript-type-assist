package codeInsight.codeGeneration;

import settings.TypeAssistApplicationSettings;

/**
 * Created by matt on 29-May-17.
 */
public interface CodeGenerator {
    String generate();

    static String removeDanglingComma(String generatedCode) {
        int lastCommaIndex = generatedCode.lastIndexOf(",");
        return generatedCode.substring(0, lastCommaIndex) + generatedCode.substring(lastCommaIndex + 1);
    }

    static String wrapInQuotesIfGeneric(String value) {
        if (value.contains("<")) {
            String quote = TypeAssistApplicationSettings.getInstance().STRING_STYLE.getStyleToken();
            return quote + value + quote;
        }
        return value;
    }
}
