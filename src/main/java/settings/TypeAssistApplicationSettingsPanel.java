package settings;

import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.JBColor;
import documentation.textReplacement.HtmlUtils;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * UI panel displayed in settings dialog.
 * <p>
 * Created by matt on 01-Jun-17.
 */
public class TypeAssistApplicationSettingsPanel {
    private JPanel settingsPanel;
    private JButton resetToDefaultsButton;

    // Code style
    private JCheckBox trailingCommasCheckBox;
    private JCheckBox endWithSemiColonCheckBox;
    private JComboBox<TypeAssistApplicationSettings.StringStyle> stringStyleComboBox;
    private JComboBox<TypeAssistApplicationSettings.VariableDeclaration> variableDeclarationComboBox;

    // Property highlighting
    private JCheckBox propertyHighlightEnabledCheckBox;
    private JComboBox<TypeAssistApplicationSettings.PropertyHighlightStyle> highlightStyleComboBox;
    private JLabel propertyHighlightColorLabel;

    // Documentation
    private JCheckBox enableSyntaxHighlightCheckbox;
    private JLabel optionalColorLabel;
    private JLabel genericsColorLabel;
    private JLabel readonlyColorLabel;
    private JLabel undefinedColorLabel;

    private JLabel optionalExampleLabel;
    private JLabel genericsExampleLabel;
    private JLabel readonlyExampleLabel;
    private JLabel undefinedExampleLabel;

    private TypeAssistApplicationSettings settings;

    public TypeAssistApplicationSettingsPanel() {
        settings = TypeAssistApplicationSettings.getInstance();
        setSettings();
    }

    private void setSettings() {
        JColorChooser colorChooser = createColorChooser();

        applySettingsToUI();

        // Constructor can be called multiple times but only want 1 listener registered.

        if (propertyHighlightColorLabel.getMouseListeners().length == 0) {
            propertyHighlightColorLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (propertyHighlightEnabledCheckBox.isSelected()) {
                        showColorChooser(colorChooser, propertyHighlightColorLabel.getBackground(),
                                ev -> propertyHighlightColorLabel.setBackground(colorChooser.getColor()));
                    }
                }
            });
        }

        if (optionalColorLabel.getMouseListeners().length == 0) {
            optionalColorLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (enableSyntaxHighlightCheckbox.isSelected()) {
                        showColorChooser(colorChooser, optionalColorLabel.getBackground(), ev -> {
                            optionalColorLabel.setBackground(colorChooser.getColor());
                            optionalExampleLabel.setText(optionalDocumentationExample(settings.toHexString(colorChooser.getColor())));
                        });
                    }
                }
            });
        }

        if (genericsColorLabel.getMouseListeners().length == 0) {
            genericsColorLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (enableSyntaxHighlightCheckbox.isSelected()) {
                        showColorChooser(colorChooser, genericsColorLabel.getBackground(), ev -> {
                            genericsColorLabel.setBackground(colorChooser.getColor());
                            genericsExampleLabel.setText(genericsDocumentationExample(settings.toHexString(colorChooser.getColor())));
                        });
                    }
                }
            });
        }

        if (readonlyColorLabel.getMouseListeners().length == 0) {
            readonlyColorLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (enableSyntaxHighlightCheckbox.isSelected()) {
                        showColorChooser(colorChooser, readonlyColorLabel.getBackground(), ev -> {
                            readonlyColorLabel.setBackground(colorChooser.getColor());
                            readonlyExampleLabel.setText(readOnlyDocumentationExample(settings.toHexString(colorChooser.getColor())));
                        });
                    }
                }
            });
        }

        if (undefinedColorLabel.getMouseListeners().length == 0) {
            undefinedColorLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (enableSyntaxHighlightCheckbox.isSelected()) {
                        showColorChooser(colorChooser, undefinedColorLabel.getBackground(), ev -> {
                            undefinedColorLabel.setBackground(colorChooser.getColor());
                            undefinedExampleLabel.setText(undefinedDocumentationExample(settings.toHexString(colorChooser.getColor())));
                        });
                    }
                }
            });
        }

        resetToDefaultsButton.addActionListener(e -> {
            settings.resetToDefault();
            applySettingsToUI();
        });
    }

    private void applySettingsToUI() {
        // Code style
        trailingCommasCheckBox.setSelected(settings.TRAILING_COMMAS);
        endWithSemiColonCheckBox.setSelected(settings.END_WITH_SEMI_COLON);
        stringStyleComboBox.setModel(new DefaultComboBoxModel<>(settings.getStringStyles()));
        stringStyleComboBox.getModel().setSelectedItem(settings.STRING_STYLE);
        variableDeclarationComboBox.setModel(new DefaultComboBoxModel<>(settings.getVariableDeclarations()));
        variableDeclarationComboBox.getModel().setSelectedItem(settings.VARIABLE_DECLARATION);

        // Property highlighting
        propertyHighlightEnabledCheckBox.setSelected(settings.PROPERTY_HIGHLIGHTING);
        enablePropertyHighlightSettings(propertyHighlightEnabledCheckBox.isSelected());

        propertyHighlightEnabledCheckBox.addActionListener(e -> {
            highlightStyleComboBox.setEnabled(propertyHighlightEnabledCheckBox.isSelected());

            if (propertyHighlightEnabledCheckBox.isSelected()) {
                setEnabledLabel(propertyHighlightColorLabel, settings.toColor(settings.PROPERTY_HIGHLIGHT_HEX_COLOR));
            } else {
                setDisabledLabel(propertyHighlightColorLabel);
            }
        });

        highlightStyleComboBox.setModel(new DefaultComboBoxModel<>(settings.getPropertyHighlightStyles()));
        highlightStyleComboBox.getModel().setSelectedItem(settings.PROPERTY_HIGHLIGHT_STYLE);
        setEnabledLabel(propertyHighlightColorLabel, settings.toColor(settings.PROPERTY_HIGHLIGHT_HEX_COLOR));

        // Documentation
        enableSyntaxHighlightCheckbox.setSelected(settings.DOCUMENTATION_SYNTAX_HIGHLIGHTING);
        enableDocumentationSettings(enableSyntaxHighlightCheckbox.isSelected());

        enableSyntaxHighlightCheckbox.addActionListener(e ->
                enableDocumentationSettings(enableSyntaxHighlightCheckbox.isSelected()));

        optionalExampleLabel.setText(optionalDocumentationExample(settings.OPTIONAL_HEX_COLOR));
        genericsExampleLabel.setText(genericsDocumentationExample(settings.GENERICS_HEX_COLOR));
        readonlyExampleLabel.setText(readOnlyDocumentationExample(settings.READONLY_HEX_COLOR));
        undefinedExampleLabel.setText(undefinedDocumentationExample(settings.UNDEFINED_HEX_COLOR));
    }

    private void enablePropertyHighlightSettings(boolean enabled) {
        highlightStyleComboBox.setEnabled(enabled);
        propertyHighlightColorLabel.setEnabled(enabled);
    }

    private void enableDocumentationSettings(boolean enabled) {
        if (enabled) {
            setEnabledLabel(optionalColorLabel, settings.toColor(settings.OPTIONAL_HEX_COLOR));
            setEnabledLabel(genericsColorLabel, settings.toColor(settings.GENERICS_HEX_COLOR));
            setEnabledLabel(readonlyColorLabel, settings.toColor(settings.READONLY_HEX_COLOR));
            setEnabledLabel(undefinedColorLabel, settings.toColor(settings.UNDEFINED_HEX_COLOR));
        } else {
            setDisabledLabel(optionalColorLabel);
            setDisabledLabel(genericsColorLabel);
            setDisabledLabel(readonlyColorLabel);
            setDisabledLabel(undefinedColorLabel);
        }

        optionalExampleLabel.setEnabled(enabled);
        genericsExampleLabel.setEnabled(enabled);
        readonlyExampleLabel.setEnabled(enabled);
        undefinedExampleLabel.setEnabled(enabled);
    }

    private void showColorChooser(JColorChooser chooser, Color initialColor, ActionListener onColorChooserOk) {
        ActionListener onColorChooserCancel = e -> {};
        chooser.setColor(initialColor);
        JColorChooser.createDialog(settingsPanel, "Choose RGB color (Alpha not supported)",
                true, chooser, onColorChooserOk, onColorChooserCancel)
        .setVisible(true);
    }

    private JColorChooser createColorChooser() {
        JColorChooser chooser = new JColorChooser();
        for (AbstractColorChooserPanel panel : chooser.getChooserPanels()) {
            if (!panel.getDisplayName().equals("RGB")) {
                chooser.removeChooserPanel(panel);
            }
        }
        return chooser;
    }

    private String optionalDocumentationExample(String hexColor) {
        return String.format("<html><code>%s: string</code></html>", HtmlUtils.span("lastName?", hexColor));
    }

    private String genericsDocumentationExample(String hexColor) {
        return String.format("<html><code>identity<%s extends string, %s>(arg1: %s, arg2: %s): %s</code></html>",
                HtmlUtils.span("T", hexColor),
                HtmlUtils.span("P", hexColor),
                HtmlUtils.span("T", hexColor),
                HtmlUtils.span("P", hexColor),
                HtmlUtils.span("T", hexColor));
    }

    private String readOnlyDocumentationExample(String hexColor) {
        return String.format("<html><code>%s firstName: string</code></html>", HtmlUtils.span("readonly", hexColor));
    }

    private String undefinedDocumentationExample(String hexColor) {
        return String.format("<html><code>%s, %s, %s, %s</code></html>",
                HtmlUtils.span("null", hexColor),
                HtmlUtils.span("undefined", hexColor),
                HtmlUtils.span("void", hexColor),
                HtmlUtils.span("never", hexColor));
    }

    /**
     * Since MouseListener is used on some of the labels, disabling a label does not disable the listener. Instead
     * the label style is changed for UI feedback.
     */
    private void setEnabledLabel(JLabel label, Color color) {
        label.setEnabled(true);
        label.setOpaque(true);
        // Fill with empty text so the label has width
        label.setText(" ");
        label.setBackground(color);
    }

    /**
     * Since MouseListener is used on some of the labels, disabling a label does not disable the listener. Instead
     * the label style is changed for UI feedback.
     */
    private void setDisabledLabel(JLabel label) {
        label.setEnabled(false);
        label.setOpaque(false);
        label.setText("disabled");
        label.setForeground(JBColor.BLACK);
    }

    public JPanel getPanel() {
        return settingsPanel;
    }

    public boolean isModified() {
        if (trailingCommasCheckBox.isSelected() != settings.TRAILING_COMMAS) return true;
        if (endWithSemiColonCheckBox.isSelected() != settings.END_WITH_SEMI_COLON) return true;
        if (stringStyleComboBox.getSelectedItem() != settings.STRING_STYLE) return true;
        if (variableDeclarationComboBox.getSelectedItem() != settings.VARIABLE_DECLARATION) return true;
        if (propertyHighlightEnabledCheckBox.isSelected() != settings.PROPERTY_HIGHLIGHTING) return true;
        if (highlightStyleComboBox.getSelectedItem() != settings.PROPERTY_HIGHLIGHT_STYLE) return true;
        if (!propertyHighlightColorLabel.getBackground().equals(settings.toColor(settings.PROPERTY_HIGHLIGHT_HEX_COLOR))) return true;
        if (enableSyntaxHighlightCheckbox.isSelected() != settings.DOCUMENTATION_SYNTAX_HIGHLIGHTING) return true;
        if (!optionalColorLabel.getBackground().equals(settings.toColor(settings.OPTIONAL_HEX_COLOR))) return true;
        if (!genericsColorLabel.getBackground().equals(settings.toColor(settings.GENERICS_HEX_COLOR))) return true;
        if (!readonlyColorLabel.getBackground().equals(settings.toColor(settings.READONLY_HEX_COLOR))) return true;
        if (!undefinedColorLabel.getBackground().equals(settings.toColor(settings.UNDEFINED_HEX_COLOR))) return true;

        return false;
    }

    public void apply() throws ConfigurationException {
        if (!isModified()) return;
        settings.TRAILING_COMMAS = trailingCommasCheckBox.isSelected();
        settings.END_WITH_SEMI_COLON = endWithSemiColonCheckBox.isSelected();
        settings.STRING_STYLE = ((TypeAssistApplicationSettings.StringStyle) stringStyleComboBox.getSelectedItem());
        settings.VARIABLE_DECLARATION = ((TypeAssistApplicationSettings.VariableDeclaration) variableDeclarationComboBox.getSelectedItem());
        settings.PROPERTY_HIGHLIGHTING = (propertyHighlightEnabledCheckBox.isSelected());
        settings.PROPERTY_HIGHLIGHT_STYLE = (TypeAssistApplicationSettings.PropertyHighlightStyle) highlightStyleComboBox.getSelectedItem();
        settings.PROPERTY_HIGHLIGHT_HEX_COLOR = settings.toHexString(propertyHighlightColorLabel.getBackground());
        settings.DOCUMENTATION_SYNTAX_HIGHLIGHTING = enableSyntaxHighlightCheckbox.isSelected();
        settings.OPTIONAL_HEX_COLOR = settings.toHexString(optionalColorLabel.getBackground());
        settings.GENERICS_HEX_COLOR = settings.toHexString(genericsColorLabel.getBackground());
        settings.READONLY_HEX_COLOR = settings.toHexString(readonlyColorLabel.getBackground());
        settings.UNDEFINED_HEX_COLOR = settings.toHexString(undefinedColorLabel.getBackground());
    }

    /**
     * Resets back to what the persistent settings are, not defaults.
     */
    public void reset() {
        setSettings();
    }


}
