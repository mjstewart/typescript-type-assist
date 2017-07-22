package settings;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Creates the UI to display in the settings dialog to persist plugin settings.
 *
 * <p>Default zero arg constructor implies application level persistence rather than project level.</p>
 *
 * <p>plugin.xml needs applicationConfigurable and applicationService registered under extensions.
 * By declaring applicationConfigurable, the default constructor will be called without the {@code Project} argument.
 * For project level persistence, change to projectConfigurable and projectService and supply 1 arg constructor
 * containing the {@code Project}</p>
 *
 * <p>applicationConfigurable storage is in users dot file eg ~/.IntellijIdea2017/system/plugins. Can also be located
 * in ~/.IntelliJIdea2017.2/system/plugins-sandbox/config/options/typescript-type-assist.xml</p>
 *
 * <p>projectConfigurable storage is the actual projects .idea folder</p>
 *
 * Created by matt on 01-Jun-17.
 */
public class TypeAssistApplicationSettingsConfigurable implements Configurable {

    private TypeAssistApplicationSettingsPanel typeAssistApplicationSettingsPanel;

    public TypeAssistApplicationSettingsConfigurable() {
        this.typeAssistApplicationSettingsPanel = new TypeAssistApplicationSettingsPanel();
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Typescript Type Assist";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    /**
     * @return the UI form to display in the settings dialog.
     */
    @Nullable
    @Override
    public JComponent createComponent() {
        return typeAssistApplicationSettingsPanel.getPanel();
    }

    /**
     * This method is regularly called to check the form for changes.
     * If the method returns false, the Apply button is disabled
     *
     * @return {@code true} if the UI form is modified implying there are new settings that need saving.
     */
    @Override
    public boolean isModified() {
        return typeAssistApplicationSettingsPanel.isModified();
    }

    /**
     * This method is called when the user clicks the OK or Apply button
     *
     * @throws ConfigurationException
     */
    @Override
    public void apply() throws ConfigurationException {
        typeAssistApplicationSettingsPanel.apply();
    }

    /**
     * This method is called when the user clicks the Cancel button.
     */
    @Override
    public void reset() {
        typeAssistApplicationSettingsPanel.reset();
    }

    /**
     * This method is called when the user closes the form, release the resources used by the form to be GC.
     */
    @Override
    public void disposeUIResources() {
        typeAssistApplicationSettingsPanel = null;
    }
}
