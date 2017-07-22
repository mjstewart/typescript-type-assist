package settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.editor.markup.EffectType;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

/**
 * Plugin settings to be persisted.
 *
 * <p>This class is the model for {@code TypeAssistApplicationSettingsPanel} which displays and applies changes
 * to these settings. See {@link TypeAssistApplicationSettingsConfigurable}</p>
 *
 * <p>See docs for which values can be persisted
 * http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html</p>
 *
 * Created by matt on 01-Jun-17.
 */
@State(
        name = "TypeScriptTypeAssistApplicationSettings",
        storages = {
                @Storage("typescript-type-assist.xml")
        }
)
public class TypeAssistApplicationSettings implements PersistentStateComponent<TypeAssistApplicationSettings> {
    // Observing jetbrains style, their settings in xml files are kept all uppercase.

    // Code style
    public boolean TRAILING_COMMAS;
    public boolean END_WITH_SEMI_COLON;
    public StringStyle STRING_STYLE;
    public VariableDeclaration VARIABLE_DECLARATION;

    // Property highlights
    public boolean PROPERTY_HIGHLIGHTING;
    public String PROPERTY_HIGHLIGHT_HEX_COLOR;
    public PropertyHighlightStyle PROPERTY_HIGHLIGHT_STYLE;

    // Documentation
    public boolean DOCUMENTATION_SYNTAX_HIGHLIGHTING;
    public String OPTIONAL_HEX_COLOR;
    public String GENERICS_HEX_COLOR;
    public String READONLY_HEX_COLOR;
    public String UNDEFINED_HEX_COLOR;


    public TypeAssistApplicationSettings() {
        /*
         * see http://www.jetbrains.org/intellij/sdk/docs/basics/persisting_state_of_components.html#persistent-component-lifecycle
         * The instance is created first, then loadState is called if there is non default state in the persisted xml.
         * loadState uses reflection to populate public fields which is why calling resetToDefault each time doesn't actually
         * reset the settings to defaults if there is non default state saved.
         */
        resetToDefault();
    }

    public void resetToDefault() {
        DefaultSettings defaultSettings = new DefaultSettings();

        TRAILING_COMMAS = defaultSettings.TRAILING_COMMAS;
        END_WITH_SEMI_COLON = defaultSettings.END_WITH_SEMI_COLON;
        STRING_STYLE = defaultSettings.STRING_STYLE;
        VARIABLE_DECLARATION = defaultSettings.VARIABLE_DECLARATION;

        PROPERTY_HIGHLIGHTING = defaultSettings.PROPERTY_HIGHLIGHTING;
        PROPERTY_HIGHLIGHT_HEX_COLOR = defaultSettings.PROPERTY_HIGHLIGHT_HEX_COLOR;
        PROPERTY_HIGHLIGHT_STYLE = defaultSettings.PROPERTY_HIGHLIGHT_STYLE;

        DOCUMENTATION_SYNTAX_HIGHLIGHTING = defaultSettings.DOCUMENTATION_SYNTAX_HIGHLIGHTING;
        OPTIONAL_HEX_COLOR = defaultSettings.OPTIONAL_HEX_COLOR;
        GENERICS_HEX_COLOR = defaultSettings.GENERICS_HEX_COLOR;
        READONLY_HEX_COLOR = defaultSettings.READONLY_HEX_COLOR;
        UNDEFINED_HEX_COLOR = defaultSettings.UNDEFINED_HEX_COLOR;
    }

    public enum StringStyle {
        SINGLE_QUOTE("'Single quotes'", "'"),
        DOUBLE_QUOTE("\"Double quotes\"", "\"");

        String presentableText;
        String styleToken;

        StringStyle(String presentableText, String styleToken) {
            this.presentableText = presentableText;
            this.styleToken = styleToken;
        }

        @Override
        public String toString() {
            return presentableText;
        }

        public String getStyleToken() {
            return styleToken;
        }
    }

    public enum PropertyHighlightStyle {
        Box("Box", EffectType.BOXED),
        Underline("Underline", EffectType.LINE_UNDERSCORE),
        DottedUnderline("Dotted Underline", EffectType.BOLD_DOTTED_LINE),
        None("None", null);

        String presentableText;
        EffectType effectType;

        PropertyHighlightStyle(String presentableText, EffectType effectType) {
            this.presentableText = presentableText;
            this.effectType = effectType;
        }

        public EffectType getEffectType() {
            return effectType;
        }

        @Override
        public String toString() {
            return presentableText;
        }
    }

    public enum VariableDeclaration {
        CONST("const"),
        LET("let"),
        VAR("var");

        String code;

        VariableDeclaration(String code) {
            this.code = code;
        }

        @Override
        public String toString() {
            return code;
        }

        public String getCode() {
            return code;
        }
    }

    /**
     * Gets the single instance managed by the ide.
     *
     * @return The {@code TypeAssistApplicationSettings}
     */
    public static TypeAssistApplicationSettings getInstance() {
        // Must be registered in plugin.xml under applicationService.
        // The ide internally manages the instance and you access the singleton through the ServiceManager
        return ServiceManager.getService(TypeAssistApplicationSettings.class);
    }

    @Nullable
    @Override
    public TypeAssistApplicationSettings getState() {
        return this;
    }

    @Override
    public void loadState(TypeAssistApplicationSettings typeAssistApplicationSettings) {
        XmlSerializerUtil.copyBean(typeAssistApplicationSettings, this);
    }

    /**
     * Supplied color converted to RGB with the hex color code is returned.
     *
     * @param color The color to convert.
     * @return The hex color code including #, eg: #F66464
     */
    public String toHexString(Color color) {
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    public Color toColor(String hexCode) {
        if (!hexCode.startsWith("#")) {
            hexCode = "#" + hexCode;
        }
        return Color.decode(hexCode);
    }

    public StringStyle[] getStringStyles() {
        return StringStyle.values();
    }

    public PropertyHighlightStyle[] getPropertyHighlightStyles() {
        return PropertyHighlightStyle.values();
    }

    public VariableDeclaration[] getVariableDeclarations() {
        return VariableDeclaration.values();
    }

    private class DefaultSettings {
        // Code style
        private boolean TRAILING_COMMAS = false;
        private boolean END_WITH_SEMI_COLON = true;
        private StringStyle STRING_STYLE = StringStyle.SINGLE_QUOTE;
        private VariableDeclaration VARIABLE_DECLARATION = VariableDeclaration.CONST;

        // Property highlighting
        private boolean PROPERTY_HIGHLIGHTING = true;
        private String PROPERTY_HIGHLIGHT_HEX_COLOR = "#F66464";
        private PropertyHighlightStyle PROPERTY_HIGHLIGHT_STYLE = PropertyHighlightStyle.Box;

        // Documentation
        private boolean DOCUMENTATION_SYNTAX_HIGHLIGHTING = true;
        private String OPTIONAL_HEX_COLOR = "#FF9090";
        private String GENERICS_HEX_COLOR = "#00FFFF";
        private String READONLY_HEX_COLOR = "#FFFF59";
        private String UNDEFINED_HEX_COLOR = "#3BFF00";
    }
}
