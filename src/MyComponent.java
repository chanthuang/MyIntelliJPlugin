import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.components.ApplicationComponent;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * Created by chant on 16/12/23.
 */
public class MyComponent implements ApplicationComponent, Configurable {

    private String KEY_WORDS = "KEY";
    private TestGUI mGUIForm;

    public MyComponent() {
    }

    @Override
    public void initComponent() {
        // TODO: insert component initialization logic here
    }

    @Override
    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @Override
    @NotNull
    public String getComponentName() {
        return "MyComponent";
    }

    public void sayHello() {
        // Show dialog with message
        Messages.showMessageDialog(PropertiesComponent.getInstance().getValue(KEY_WORDS),"Sample",Messages.getInformationIcon());
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "chant DisplayName";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return "chant Help Topic";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (mGUIForm == null) {
            mGUIForm = new TestGUI();
        }
        return mGUIForm.getRootComponent();
    }

    @Override
    public boolean isModified() {
        return true;
    }

    @Override
    public void apply() throws ConfigurationException {
        if (mGUIForm != null) {
            PropertiesComponent.getInstance().setValue(KEY_WORDS, mGUIForm.getWords());
        }
    }

    @Override
    public void reset() {
    }
}
