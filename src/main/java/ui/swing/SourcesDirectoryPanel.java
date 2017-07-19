package ui.swing;

import javax.swing.*;

/**
 * Created by dd on 16.07.17.
 */
public class SourcesDirectoryPanel {
    private JTextField descriptionText;
    private JCheckBox activeCheckBox;
    private JButton deleteButton;

    public JTextField getDescriptionText() {
        return descriptionText;
    }

    public JCheckBox getActiveCheckBox() {
        return activeCheckBox;
    }

    public JButton getDeleteButton() {
        return deleteButton;
    }

    public JPanel getContentPane() {
        return contentPane;
    }

    private JPanel contentPane;
    private JProgressBar loadingBar;
    private JPanel loadedContent;
    private JPanel loadingContent;
    private JButton cancelLoadButton;


    public JProgressBar getLoadingBar() {
        return loadingBar;
    }

    public JPanel getLoadedContent() {
        return loadedContent;
    }

    public JPanel getLoadingContent() {
        return loadingContent;
    }

    public JButton getCancelLoadingButton() {
        return cancelLoadButton;
    }
}
