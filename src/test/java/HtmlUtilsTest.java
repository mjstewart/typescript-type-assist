import documentation.HtmlUtils;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * Created by matt on 01-Jul-17.
 */
public class HtmlUtilsTest {

    @Test
    public void bold() {
        String value = "hello";
        assertThat(HtmlUtils.bold(value), is(String.format("<b>%s</b>", value)));
    }

    @Test
    public void horizontalLine() {
        assertThat(HtmlUtils.horizontalLine(), is("<hr>"));
    }

    @Test
    public void spanColored_WithHexColorHash() {
        // hexColor has a #.
        String value = "hello";
        String hexColor = "#2EFE2E";
        String expected = String.format("<span style=\"color:%s\">%s</span>", hexColor, value);
        assertThat(HtmlUtils.span(value, hexColor), is(expected));
    }

    @Test
    public void spanColored_NoHexColorHash() {
        // If hexColor has no #, the span method must prepend one on.
        String value = "hello";
        String hexColor = "2EFE2E";
        String expected = String.format("<span style=\"color:#%s\">%s</span>", hexColor, value);
        assertThat(HtmlUtils.span(value, hexColor), is(expected));
    }

    @Test
    public void spanNoStyle() {
        String value = "hello";
        String expected = String.format("<span>%s</span>", value);
        assertThat(HtmlUtils.span(value), is(expected));
    }

    @Test
    public void heading() {
        String value = "hello";
        String expected = String.format("<h3 style=\"text-decoration:underline\">%s</h3>", value);
        assertThat(HtmlUtils.heading(value), is(expected));
    }

    @Test
    public void newLine() {
        assertThat(HtmlUtils.newLine(), is("<br>"));
    }

    @Test
    public void code() {
        String value = "hello";
        String expected = String.format("<code>%s</code>", value);
        assertThat(HtmlUtils.code(value), is(expected));
    }
}