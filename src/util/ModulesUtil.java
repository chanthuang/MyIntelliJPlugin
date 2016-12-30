package util;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.file.PsiDirectoryFactory;

import java.util.ArrayList;
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

    public List<PsiDirectory> getModuleValuesDir() {
        List<PsiDirectory> valuesDirs = new ArrayList<>();
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
