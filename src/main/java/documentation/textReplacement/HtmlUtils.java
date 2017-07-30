package documentation.textReplacement;

import java.util.function.Function;

/**
 * Created by matt on 30-Jun-17.
 */
public class HtmlUtils {
    public static String bold(String text) {
        return String.format("<b>%s</b>", text);
    }

    public static String horizontalLine() {
        return "<hr>";
    }

    /**
     * Creates a colored span based on supplied {@code hexColor}.
     *
     * @param hexColor The hex color code such as "#FF0000". "FF0000" can be supplied and the "#" will be prepended.
     */
    public static String span(String value, String hexColor) {
        if (!hexColor.startsWith("#")) {
            hexColor = "#" + hexColor;
        }
        return html()
                .apply("span")
                .apply("color:" + hexColor)
                .apply(value);
    }

    public static String span(String value) {
        return html()
                .apply("span")
                .apply("")
                .apply(value);
    }

    public static String heading(String text) {
        return html()
                .apply("h3")
                .apply("text-decoration:underline")
                .apply(text);
    }

    public static String newLine() {
        return "<br>";
    }

    public static String code(String value) {
        return String.format("<code>%s</code>", value);
    }


    private static Function<String, Function<String, Function<String, String>>> html() {
        return tag -> style -> value -> {
            if (style.isEmpty()) {
                return String.format("<%s>%s</%s>", tag, value, tag);
            }
            return String.format("<%s style=\"%s\">%s</%s>", tag, style, value, tag);
        };
    }
}
