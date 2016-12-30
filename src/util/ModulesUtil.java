package util;

import com.google.common.base.Joiner;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.PsiDirectoryFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModulesUtil {
    private Project project;
    private boolean isAndroidProject = false;

    public ModulesUtil(Project project) {
        this.project = project;
    }

    public Set<String> getModules() {
        Set<String> modules = new HashSet<String>();
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(project.getBaseDir());
        if (isAndroidProject(baseDir)) {
            Logger.debug(project.getName() + " is an Android project");
            PsiDirectory[] dirs = baseDir.getSubdirectories();
            for (PsiDirectory dir : dirs) {
                if (!dir.getName().equals("build") && !dir.getName().equals("gradle")) {
                    if (isModule(dir)) {
                        Logger.debug(dir.getName() + " is a Module");
                        modules.add(dir.getName());
                    }
                }
            }
        }
        Logger.debug(modules.toString());
        return modules;
    }

    private Set<PsiDirectory> getModuleValuesDir() {
        Set<PsiDirectory> valuesDirs = new HashSet<>();
        Set<String> modules = getModules();
        for (String moduleName : modules) {
            PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(project.getBaseDir());
            PsiDirectory moduleDir = baseDir.findSubdirectory(moduleName);
            if (moduleDir != null && moduleDir.isDirectory()) {
                PsiDirectory srcDir = moduleDir.findSubdirectory("src");
                if (srcDir != null && srcDir.isDirectory()) {
                    PsiDirectory mainDir = srcDir.findSubdirectory("main");
                    if (mainDir != null && mainDir.isDirectory()) {
                        PsiDirectory resDir = mainDir.findSubdirectory("res");
                        if (resDir != null && resDir.isDirectory()) {
                            PsiDirectory valuesDir = resDir.findSubdirectory("values");
                            if (valuesDir != null && valuesDir.isDirectory()) {
                                valuesDirs.add(valuesDir);
                            }
                        }
                    }
                }
            }
        }
        return valuesDirs;
    }

    private static final String CUSTOM_VALUES_DIR_KEY = "CustomValuesDirKey";
    private static final String CUSTOM_VALUES_DIR_SEPARATOR = "_customValuesDirSeparator_";

    public void setCustomValuesDirProperty(@Nullable List<String> filePaths) {
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

    public
    @NotNull
    String[] getCustomValuesDirProperty() {
        PropertiesComponent properties = PropertiesComponent.getInstance(project);
        String valuesString = properties.getValue(CUSTOM_VALUES_DIR_KEY, "");
        return valuesString.split(CUSTOM_VALUES_DIR_KEY);
    }

    private Set<PsiDirectory> getCustomValuesDir() {
        Set<PsiDirectory> valuesDirs = new HashSet<>();
        String[] filePaths = getCustomValuesDirProperty();
        for (String filePath : filePaths) {
            if (filePath != null && filePath.length() > 0) {
                VirtualFile valuesDir = LocalFileSystem.getInstance().findFileByPath(filePath);
                if (valuesDir != null && valuesDir.isDirectory()) {
                    valuesDirs.add(PsiDirectoryFactory.getInstance(project).createDirectory(valuesDir));
                }
            }
        }
        return valuesDirs;
    }

    public Set<String> getAllValueFilesPath() {
        Set<String> results = new HashSet<>();
        Set<PsiDirectory> valuesDirs = getModuleValuesDir();
        valuesDirs.addAll(getCustomValuesDir());
        for (PsiDirectory valuesDir : valuesDirs) {
            PsiFile[] files = valuesDir.getFiles();
            for (PsiFile file : files) {
                if (file.getName().endsWith(".xml")) {
                    results.add(file.getVirtualFile().getPath());
                }
            }
        }
        return results;
    }

    public PsiDirectory getResDir(String moduleName) {
        PsiDirectory baseDir = PsiDirectoryFactory.getInstance(project).createDirectory(project.getBaseDir());
        PsiDirectory moduleDir = baseDir.findSubdirectory(moduleName);
        if (moduleDir != null && moduleDir.isDirectory()) {
            PsiDirectory srcDir = moduleDir.findSubdirectory("src");
            if (srcDir != null && srcDir.isDirectory()) {
                PsiDirectory mainDir = srcDir.findSubdirectory("main");
                if (mainDir != null && mainDir.isDirectory()) {
                    PsiDirectory resDir = mainDir.findSubdirectory("res");
                    if (resDir != null && resDir.isDirectory()) {
                        return resDir;
                    }
                }
            }
        }
        return null;
    }

    private boolean isAndroidProject(PsiDirectory directory) {
        PsiFile[] files = directory.getFiles();
        for (PsiFile file : files) {
            if (file.getName().equals("build.gradle")) {
                isAndroidProject = true;
                return true;
            }
        }
        isAndroidProject = false;
        return false;
    }

    public boolean isAndroidProject() {
        Logger.debug("Is Android project:" + isAndroidProject);
        return isAndroidProject;
    }

    private boolean isModule(PsiDirectory directory) {
        boolean hasGradle = false;
        boolean hasSrc = false;
        PsiFile[] files = directory.getFiles();
        PsiDirectory[] dirs = directory.getSubdirectories();
        for (PsiFile file : files) {
            if (file.getName().equals("build.gradle")) {
                hasGradle = true;
                break;
            }
        }

        for (PsiDirectory dir : dirs) {
            if (dir.getName().equals("src")) {
                hasSrc = true;
                break;
            }
        }
        return hasGradle && hasSrc;
    }
}
