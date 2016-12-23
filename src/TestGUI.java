import javax.swing.*;

/**
 * Created by chant on 16/12/23.
 */
public class TestGUI {
    private JTextField textField1;
    private JLabel mLabel;
    private JPanel mPanel;

    public TestGUI() {
        mLabel.setText("test");
    }

    public JComponent getRootComponent() {
        return mPanel;
    }

    public String getWords() {
        return textField1.getText();
    }
}
