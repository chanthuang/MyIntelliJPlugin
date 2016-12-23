import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by chant on 16/12/23.
 */
public class FindAttrValueAction extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {

        /**
         * 1. 遍历所有文件的所有行
         * 2. 找 <item name="xxx"></item>
         * 3. 如果只有一个结果，直接跳转到这个文件的这一行
         * 4. 如果有多个结果，弹出菜单列出xxx，点击菜单项时跳转到这个文件的这一行
         */

        final String filePath = "/Users/chant/android/qmuidemo_android/qmui/src/main/res/values/qmui_themes.xml";
        final String attrName = "qmui_btn_text_size";
        // 1. 遍历所有文件的所有行
        List<String> matchLines = findAttrNameInFile(filePath, attrName);
    }

    private List<String> findAttrNameInFile(String filePath, String attrName) {
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
                    System.out.println("attr=" + lineAttrName + " ---> " + line);
                    if (lineAttrName.equals(attrName)) {
                        lines.add(line);
                    }
                } else {
                    System.out.println("不能识别 ---> " + line);
                }

            }

//        return lines;
        } catch (Exception exception) {
            System.out.println("error findAttrNameInFile: " + exception.getMessage());
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
