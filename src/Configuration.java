import com.google.common.base.Joiner;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
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
        valuesDirsPath = getValuesDir(getProject());
        addButton.addActionListener(e -> {
            PsiDirectory dir = showDirChooser(getProject());
            if (dir != null) {
                String filePath = dir.getVirtualFile().getPath();
                valuesDirsPath.add(0, filePath);
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
        // TODO chant 通知 List 更新的方法是这样吗？
        valuesFolderList.setListData(valuesDirsPath.toArray());
    }

    private Project getProject() {
        // TODO chant 让 ProjectUtil 去 "guess" 似乎不太好
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
    @Nullable
    List<String> getCustomValuesDir(Project project) {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String valuesString = properties.getValue(CUSTOM_VALUES_DIR_KEY, "");
        if (valuesString.isEmpty()) {
            return null;
        } else {
            String[] dirs = valuesString.split(CUSTOM_VALUES_DIR_SEPARATOR);
            List<String> result = new ArrayList<>();
            for (String dir : dirs) {
                result.add(dir);
            }
            return result;
        }
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

    private static List<String> getValuesDir(Project project) {
        List<String> dirs = getCustomValuesDir(project);
        if (dirs != null && dirs.size() > 0) {
            // 有设置过
            return dirs;
        } else {
            // 没有设置过
            return getDefaultValuesDir(project);
        }
    }

    static List<String> getAllValueFilesPath(Project project) {
        List<String> results = new ArrayList<>();
        List<String> valuesDirs = getValuesDir(project);
        for (String valuesDir : valuesDirs) {
            VirtualFile virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(valuesDir);
            if (virtualFile != null) {
                PsiDirectory directory = PsiDirectoryFactory.getInstance(project).createDirectory(virtualFile);
                PsiFile[] files = directory.getFiles();
                for (PsiFile file : files) {
                    if (file.getName().endsWith(".xml")) {
                        results.add(file.getVirtualFile().getPath());
                    }
                }
            }
        }
        return results;
    }


}
