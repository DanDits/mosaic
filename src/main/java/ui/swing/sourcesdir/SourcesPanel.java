package ui.swing.sourcesdir;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dd on 19.07.17.
 */
public class SourcesPanel implements SourcesDirectoryChangeListener {
    private JTextField sourcesStatusText;
    private JButton chooseDirectoryButton;
    private JScrollPane sourcesDirectoryContainerScrollPane;
    private JPanel sourcesDirectoryContainer;
    private JPanel contentPane;
    private SourcesController controller;
    private Set<SourcesDirectoryPanel> panels = new HashSet<>();

    public SourcesPanel() {
        controller = new SourcesController();
        sourcesDirectoryContainer.setLayout(new GridLayout(0, 1));
        sourcesDirectoryContainerScrollPane.getViewport().setOpaque(false);
        chooseDirectoryButton.addActionListener(actionEvent -> chooseDirectory());
    }

    private void chooseDirectory() {
        JFileChooser jfc = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory());
        jfc.setDialogTitle("Wählen Ordner mit vielen Bildern");
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        jfc.setMultiSelectionEnabled(true);
        int returnValue = jfc.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = jfc.getSelectedFiles();
            controller.chooseSourcesDirectories(selectedFiles, this);
        }
    }
    // TODO colors: http://paletton.com/#uid=73f0x0kimmu8oy6dlrnmsirrSd3

    private SourcesDirectoryPanel getSourcesDirectoryPanel(SourcesDirectory directory) {
        for (SourcesDirectoryPanel panel : panels) {
            if (panel.getDirectory().equals(directory)) {
                return panel;
            }
        }
        return null;
    }

    @Override
    public void onSourcesDirectoryCreated(SourcesDirectory directory) {
        System.out.println("Directory created:" + directory);
        final SourcesDirectoryPanel panel = new SourcesDirectoryPanel();
        panel.init(directory, controller);
        int index = controller.getSourcesDirectoryIndex(directory);
        panels.add(panel);
        System.out.println("Index for directory:" + index);
        sourcesDirectoryContainer.add(panel.getContentPane(), index);
        sourcesDirectoryContainer.revalidate();
        sourcesDirectoryContainer.repaint();
    }


    @Override
    public void onSourcesDirectoryStatusChange(SourcesDirectory directory) {
        System.out.println("Directory changed status:" + directory);
        SourcesDirectoryPanel panel = getSourcesDirectoryPanel(directory);
        if (panel != null) {
            panel.update();
            sourcesDirectoryContainer.revalidate();
            sourcesDirectoryContainer.repaint();
        }
    }

    @Override
    public void onSourcesDirectoryRemoved(SourcesDirectory directory) {
        System.out.println("Directory removed:" + directory);
        int index = controller.getSourcesDirectoryIndex(directory);
        sourcesDirectoryContainer.remove(index);
        panels.remove(getSourcesDirectoryPanel(directory));
        sourcesDirectoryContainer.revalidate();
        sourcesDirectoryContainer.repaint();
    }

    public Component getContentPane() {
        return contentPane;
    }
}
