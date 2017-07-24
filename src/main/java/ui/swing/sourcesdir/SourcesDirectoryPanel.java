package ui.swing.sourcesdir;

import javax.swing.*;
import java.awt.event.ItemEvent;

/**
 * Created by dd on 16.07.17.
 */
public class SourcesDirectoryPanel {
    private JTextField descriptionText;
    private JCheckBox activeCheckBox;
    private JButton deleteButton;

    private JPanel contentPane;
    private JProgressBar loadingBar;
    private JPanel loadedContent;
    private JPanel loadingContent;
    private JButton cancelLoadButton;
    private SourcesDirectory directory;

    SourcesDirectoryPanel() {
    }

    void init(SourcesDirectory directory, SourcesController controller) {
        this.directory = directory;
        boolean loaded = directory.isLoaded();
        loadingContent.setVisible(!loaded);
        loadedContent.setVisible(loaded);
        updateDescription();
        activeCheckBox.addItemListener(itemEvent -> controller.setSourcesDirectoryActive(directory,
                                                                           itemEvent.getStateChange() == ItemEvent.SELECTED));
        updateActiveState();
        deleteButton.addActionListener(actionEvent -> controller.removeSourcesDirectory(directory));

        cancelLoadButton.addActionListener(actionEvent -> controller.cancelLoadDirectory(directory));
        if (!loaded) {
            loadingBar.setToolTipText(directory.getDescription());
            controller.loadDirectory(directory, progress -> loadingBar.setValue(progress));
        }
    }

    private void updateDescription() {
        descriptionText.setText(directory.getDescription());
    }

    private void updateActiveState() {
        activeCheckBox.setSelected(directory.isActive());
    }

    JPanel getContentPane() {
        return contentPane;
    }

    SourcesDirectory getDirectory() {
        return directory;
    }

    void update() {
        updateDescription();
        updateActiveState();
        if (directory.isLoaded()) {
            loadingContent.setVisible(false);
            loadedContent.setVisible(true);
        }
    }
}
