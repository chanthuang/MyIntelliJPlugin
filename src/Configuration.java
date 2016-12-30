import com.google.common.base.Joiner;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.ui.popup.list.ListPopupImpl;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import util.ModulesUtil;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Configuration implements Configurable {

    private JPanel root;
    private JList valuesFolderList;
    private JButton addButton;
    private JButton resetButton;

    private List<String> valuesDirsPath = new ArrayList<>();
    private boolean isModified = false;

    public Configuration() {
    }

    @Nls
    @Override
    public String getDisplayName() {
        return "Find attr name";
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        return null;
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        valuesFolderList.addListSelectionListener(e -> {
        });
        addButton.addActionListener(e -> {
            PsiDirectory dir = showDirChooser(getProject());
            if (dir != null) {
                valuesDirsPath.add(0, dir.getVirtualFile().getPath());
                notifyList();
                valuesFolderList.setSelectedIndex(0);
                isModified = true;
            }
        });
        resetButton.addActionListener(e -> {
            valuesDirsPath = getDefaultValuesDir(getProject());
            notifyList();
            isModified = true;
        });
        return root;
    }

    private void notifyList() {
        valuesFolderList.setListData(valuesDirsPath.toArray());
    }

    private Project getProject() {
        return ProjectUtil.guessCurrentProject(root);
    }

    @Override
    public boolean isModified() {
        return isModified;
    }

    @Override
    public void apply() throws ConfigurationException {
        setCustomValuesDirProperty(getProject(), valuesDirsPath);
        isModified = false;
    }

    @Override
    public void reset() {
        valuesDirsPath = getValuesDir(getProject());
        notifyList();
        isModified = false;
    }

    private PsiDirectory showDirChooser(Project project) {
        FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFolderDescriptor();
        VirtualFile virtualFile = FileChooser.chooseFile(descriptor, project, null);
        if (virtualFile != null) {
            if (virtualFile.isDirectory()) {
                return PsiManager.getInstance(project).findDirectory(virtualFile);
            }
        }
        return null;
    }

    private static final String CUSTOM_VALUES_DIR_KEY = "CustomValuesDirKey";
    private static final String CUSTOM_VALUES_DIR_SEPARATOR = "_customValuesDirSeparator_";

    private void setCustomValuesDirProperty(Project project, @Nullable List<String> filePaths) {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        if (filePaths != null && filePaths.size() > 0) {
            String value = Joiner.on(CUSTOM_VALUES_DIR_SEPARATOR).join(filePaths);
            properties.setValue(CUSTOM_VALUES_DIR_KEY, value);
        } else {
            if (properties.isValueSet(CUSTOM_VALUES_DIR_KEY)) {
                properties.unsetValue(CUSTOM_VALUES_DIR_KEY);
            }
        }
    }

    private static
    @NotNull
    List<String> getCustomValuesDirProperty(Project project) {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String valuesString = properties.getValue(CUSTOM_VALUES_DIR_KEY, "");
        String[] dirs = valuesString.split(CUSTOM_VALUES_DIR_SEPARATOR);
        return Arrays.asList(dirs);
    }

    private static List<String> getDefaultValuesDir(Project project) {
        ModulesUtil moduleUtil = new ModulesUtil(project);
        List<PsiDirectory> valuesDirs = moduleUtil.getModuleValuesDir();
        List<String> valuesDirsPath = new ArrayList<>();
        for (PsiDirectory dir : valuesDirs) {
            valuesDirsPath.add(dir.getVirtualFile().getPath());
        }
        return valuesDirsPath;
    }

    public static List<String> getValuesDir(Project project) {
        List<String> dirs = getCustomValuesDirProperty(project);
        if (dirs.size() > 0) {
            // 有设置过
            return dirs;
        } else {
            // 没有设置过
            return getDefaultValuesDir(project);
        }
    }

}
