import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.CodeInsightAction;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import util.Logger;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

                int caretOffset = editor.getCaretModel().getOffset();
                PsiElement psiElement = psiFile.findElementAt(caretOffset);
                if (psiElement == null) {
                    Logger.error("Error: psiElement == null");
                    return;
                }

                String elementText = psiElement.getText(); // 期望的值是 ?attr/xxxx

                if (!elementText.startsWith("?attr/")) {
                    Logger.error("Error: attrName(" + elementText + ") is not starts with ?attr/");
                    return;
                }

                String attrName = elementText.replace("?attr/", "");

                List<String> allValuesFilesPath = Configuration.getAllValueFilesPath(project);
                List<LineMatchResult> allMatchLines = findAttrName(allValuesFilesPath, attrName);

                if (allMatchLines.size() == 0) {
                    NotificationGroup NOTIFICATION_GROUP = NotificationGroup.balloonGroup("chant");
                    NOTIFICATION_GROUP.createNotification("chant 报错",
                            "没有找到 " + elementText + " 的赋值位置",
                            NotificationType.ERROR,
                            null
                    ).notify(project);
                } else if (allMatchLines.size() == 1) {
                    LineMatchResult fileMatchResult = allMatchLines.get(0);
                    openFile(project, fileMatchResult.filePath, fileMatchResult.lineIndex, fileMatchResult.startIndex);
                } else if (allMatchLines.size() > 1) {
                    // 弹出菜单选择要打开的文件
                    LogicalPosition logicalPosition = editor.offsetToLogicalPosition(caretOffset);
                    Point xy = editor.logicalPositionToXY(logicalPosition);
                    showMenu(project, editor.getComponent(), xy.x, xy.y + editor.getLineHeight(), allMatchLines);
                }
            }

            @Override
            public boolean startInWriteAction() {
                return false;
            }
        };
    }

    private
    @NotNull
    List<LineMatchResult> findAttrName(List<String> resFiles, String attrName) {
        Logger.debug("[findAttrName] attrName = " + attrName);

        //  1. 遍历所有文件的所有行找<item name="attrName">***</item>
        //  2. 如果只有一个结果，直接跳转到这个文件的这一行
        //  3. 如果有多个结果，弹出菜单列出来，点击菜单项时跳转到这个文件的这一行

        List<LineMatchResult> allMatchLines = new ArrayList<>();
        for (String filePath : resFiles) {
            List<LineMatchResult> matchLines = findAttrNameInFile(filePath, attrName);
            if (matchLines.size() > 0) {
                allMatchLines.addAll(matchLines);

                Logger.debug("match: "
                        + "\t"
                        + filePath
                        + "matchLines="
                        + String.valueOf(matchLines.size()));
            } else {
                Logger.debug("Not match");
            }
        }

        return allMatchLines;

    }

    private void showMenu(Project project, Component component, int x, int y, List<LineMatchResult> matchLines) {
        DefaultActionGroup group = new DefaultActionGroup();
        for (LineMatchResult line : matchLines) {
            group.addAction(new AnAction(line.filePath, line.filePath, null) {
                @Override
                public void actionPerformed(AnActionEvent anActionEvent) {
                    openFile(project, line.filePath, line.lineIndex, line.startIndex);
                }
            });
        }
        ActionPopupMenu menu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.ANT_MESSAGES_POPUP, group);
        menu.getComponent().show(component, x, y);
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
                                    filePath,
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
         * 文件路径
         */
        String filePath;
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

        LineMatchResult(String filePath, int lineIndex, String lineString, String nameValue, int startIndex) {
            this.filePath = filePath;
            this.lineIndex = lineIndex;
            this.lineString = lineString;
            this.nameValue = nameValue;
            this.startIndex = startIndex;
        }
    }

}
