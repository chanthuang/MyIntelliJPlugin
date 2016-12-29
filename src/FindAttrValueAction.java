import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import util.Logger;
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

                Logger.init("chant", Logger.DEBUG);

                ModulesUtil modulesUtil = new ModulesUtil(project);

                int caretOffset = editor.getCaretModel().getOffset();
                PsiElement psiElement = psiFile.findElementAt(caretOffset);
                if (psiElement == null) {
                    Logger.error("Error: psiElement == null");
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
        Logger.debug("[findAttrName] attrName = " + attrName);

        if (!attrName.startsWith("?attr/")) {
            Logger.error("Error: attrName(" + attrName + ") is not starts with ?attr/");
            return;
        } else {
            attrName = attrName.replace("?attr/", "");
        }

        //  1. 遍历所有文件的所有行找<item name="attrName"></item>
        //  2. 如果只有一个结果，直接跳转到这个文件的这一行
        //  3. 如果有多个结果，弹出菜单列出xxx，点击菜单项时跳转到这个文件的这一行

        List<String> allMatchesFile = new ArrayList<>();
        List<List<LineMatchResult>> allMatchesLineInFile = new ArrayList<>();
        for (String filePath : resFiles) {
            List<LineMatchResult> matchLines = findAttrNameInFile(filePath, attrName);
            if (matchLines.size() > 0) {
                allMatchesFile.add(filePath);
                allMatchesLineInFile.add(matchLines);

                Logger.debug("match: "
                        + "\t"
                        + filePath
                        + "matchLines="
                        + String.valueOf(matchLines.size()));
            } else {
                Logger.debug("Not match");
            }
        }

        if (allMatchesFile.size() == 1) {
            String filePath = allMatchesFile.get(0);
            LineMatchResult firstMatchLine = allMatchesLineInFile.get(0).get(0);
            openFile(project, filePath, firstMatchLine.lineIndex, firstMatchLine.startIndex);
        } else if (allMatchesFile.size() > 1) {
            // TODO show menu and open file
            for (int i = 0; i < allMatchesFile.size(); i++) {
                String filePath = allMatchesFile.get(i);
                LineMatchResult firstMatchLine = allMatchesLineInFile.get(i).get(0);
                openFile(project, filePath, firstMatchLine.lineIndex, firstMatchLine.startIndex);
            }
        }
    }

    /**
     * 打开文件，移动光标到指定行号和指定列号
     */
    private void openFile(Project project, String filePath, int line, int column) {
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(filePath);
        if (file != null) {
            new OpenFileDescriptor(project, file, line, column).navigate(true);
        } else {
            Logger.error("Error: [openFile] file from " + filePath + " is null");
        }
    }

    private static final Pattern nameValuePattern = Pattern.compile("<item.*name ?= ?\\\"(.*?)\\\"");

    private List<LineMatchResult> findAttrNameInFile(String filePath, String attrName) {
        Logger.debug("[findAttrNameInFile] filePath=" + filePath + ", attrName=" + attrName);

        List<LineMatchResult> lines = new ArrayList<>();
        if (filePath == null || attrName == null) {
            return lines;
        }
        try {
            File file = new File(filePath);
            BufferedReader bufReader = new BufferedReader(new FileReader(file));
            String lineString;
            int lineIndex = 0;
            while ((lineString = bufReader.readLine()) != null) {
                Matcher matcher = nameValuePattern.matcher(lineString);
                if (matcher.find()) {
                    String nameValue = matcher.group(1).trim();
                    if (nameValue.length() > 0) {
                        if (nameValue.equals(attrName)) {
                            LineMatchResult lineResult = new LineMatchResult(
                                    lineIndex,
                                    lineString,
                                    nameValue,
                                    matcher.start(1));
                            lines.add(lineResult);
                        }
                    }
                }
                lineIndex++;
            }
        } catch (Exception exception) {
            Logger.error("Error: findAttrNameInFile: " + exception.getMessage());
        }
        return lines;
    }

    private static class LineMatchResult {
        /**
         * 在文件中的行号
         */
        int lineIndex;
        /**
         * 这一行的字符串
         */
        String lineString;
        /**
         * name属性的值
         */
        String nameValue;
        /**
         * name属性的值在这一行的 startIndex
         */
        int startIndex;

        LineMatchResult(int lineIndex, String lineString, String nameValue, int startIndex) {
            this.lineIndex = lineIndex;
            this.lineString = lineString;
            this.nameValue = nameValue;
            this.startIndex = startIndex;
        }
    }

}
