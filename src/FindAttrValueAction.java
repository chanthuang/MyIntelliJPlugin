import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import util.ModulesUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chant on 16/12/23.
 */
public class FindAttrValueAction extends CodeInsightAction {

    @NotNull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new CodeInsightActionHandler() {
            @Override
            public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile psiFile) {

                ModulesUtil modulesUtil = new ModulesUtil(project);

                // TODO 判断是否 Android Project
//                if (!modulesUtil.isAndroidProject()) {
//                    return;
//                }

                /**
                 * 1. 遍历所有文件的所有行找<item name="xxx"></item>
                 * 2. 如果只有一个结果，直接跳转到这个文件的这一行
                 * 3. 如果有多个结果，弹出菜单列出xxx，点击菜单项时跳转到这个文件的这一行
                 */

                int caretOffset = editor.getCaretModel().getOffset();
                PsiElement psiElement = psiFile.findElementAt(caretOffset);
                if (psiElement == null) {
                    System.out.println("Error: psiElement == null");
                    return;
                }

                String elementName = psiElement.getText();
                Set<String> allValuesFilesPath = modulesUtil.getAllValueFilesPath();
                findAttrName(project, allValuesFilesPath, elementName);
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    private void findAttrName(Project project, Set<String> resFiles, String attrName) {
        System.out.println("[findAttrName] attrName = " + attrName);

        if (!attrName.startsWith("?attr/")) {
            System.out.println("Error: attrName(" + attrName + ") is not starts with ?attr/");
            return;
        } else {
            attrName = attrName.replace("?attr/", "");
        }

        List<String> allMatchesFile = new ArrayList<>();
        for (String filePath : resFiles) {
            List<String> matchLines = findAttrNameInFile(filePath, attrName);
            if (matchLines.size() > 0) {
                allMatchesFile.add(filePath);
                StringBuilder sb = new StringBuilder("match: --- ");
                for (String line : matchLines) {
                    sb.append("\n").append(line);
                }
                System.out.println(sb);
            } else {
                System.out.println("Not match");
            }
        }

        if (allMatchesFile.size() == 1) {
            openFile(project, allMatchesFile.get(0));
        } else if (allMatchesFile.size() > 1) {
            // TODO show menu and open file
            for (String filePath : allMatchesFile) {
                openFile(project, filePath);
            }
        }
    }

    private void openFile(Project project, String filePath) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        VirtualFile vf = LocalFileSystem.getInstance().findFileByPath(filePath);
        fileEditorManager.openFile(vf, true, true);
    }

    private List<String> findAttrNameInFile(String filePath, String attrName) {
        System.out.println("[findAttrNameInFile] filePath=" + filePath + ", attrName=" + attrName);

        List<String> lines = new ArrayList<>();
        if (filePath == null || attrName == null) {
            return lines;
        }
        try {
            File file = new File(filePath);
            BufferedReader bufReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = bufReader.readLine()) != null) {
                String lineAttrName = getNameFromLine(line);
                if (lineAttrName.length() > 0) {
//                    System.out.println("attr=" + lineAttrName + " ---> " + line);
                    if (lineAttrName.equals(attrName)) {
                        lines.add(line);
                    }
                } else {
//                    System.out.println("不能识别 ---> " + line);
                }

            }

//        return lines;
        } catch (Exception exception) {
            System.out.println("Error: findAttrNameInFile: " + exception.getMessage());
        }
        return lines;
    }

    private static final Pattern pattern = Pattern.compile("<item.*name ?= ?\\\"(.*?)\\\"");

    private
    @NotNull
    String getNameFromLine(String content) {
        Matcher matcher = pattern.matcher(content);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return "";
    }

}
