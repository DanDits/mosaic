package ui.swing;

import ui.swing.controller.EasyMosaicController;
import ui.swing.controller.SourcesDirectory;
import ui.swing.controller.SourcesDirectoryChangeListener;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Hashtable;

/**
 * Created by dd on 29.06.17.
 */
public class EasyMosaicGuide implements SourcesDirectoryChangeListener {
    private EasyMosaicController controller;
    private JPanel mosaic_file_panel;
    private JPanel reconstructor_panel;
    private JTextField mosaic_main_info_text;
    private JPanel file_panel;
    private JTextField a100BilderAusHandyTextField;
    private JButton chooseDirectoryButton;
    private JTextField effect_main_info_text;
    private JPanel main_effect_panel;
    private JPanel options;
    private JTextField mosaicCreationStatusTextField;
    private JButton chooseSourceImageButton;
    private JProgressBar globalProgressBar;
    private JRadioButton preferFormatMatcherRadioButton;
    private JRadioButton preferColorMatcherRadioButton;
    private JRadioButton chooseRandomlyRadioButton;
    private JCheckBox noImageReuseCheckBox;
    private JPanel contentPane;
    private JSlider fineToRoughSlider;
    private JButton saveButton;
    private JScrollPane sourcesDirectoryContainerScrollPane;
    private JPanel sourcesDirectoryContainer;
    private ArrayList<SourcesDirectoryPanel> sourcesDiretoryPanes = new ArrayList<>();

    private EasyMosaicGuide() {
        controller = new EasyMosaicController();
        addSimpleBorder(options);
        addSimpleBorder(file_panel);
        addSimpleBorder(main_effect_panel);

        initNoDuplicates();
        initMatcherOptions();

        chooseDirectoryButton.addActionListener(actionEvent -> chooseDirectory());
        initFineRoughSlider();
        mosaic_main_info_text.setFont(new Font(mosaic_main_info_text.getFont().getFontName(), Font.BOLD, 25));
        sourcesDirectoryContainerScrollPane.getViewport().setOpaque(false);
        sourcesDirectoryContainer = new JPanel();
        sourcesDirectoryContainer.setLayout(new GridLayout(0, 1));
        sourcesDirectoryContainer.setOpaque(false);
        sourcesDirectoryContainerScrollPane.getViewport().add(sourcesDirectoryContainer);
    }

    private static void addSimpleBorder(JComponent comp) {
        int width = 2;
        comp.setBorder(BorderFactory.createMatteBorder(width, width, width, width, Color.BLACK));
    }

    private void initFineRoughSlider() {
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put(0, new JLabel("Fein"));
        labelTable.put( 100, new JLabel("Grob"));

        fineToRoughSlider.setPaintLabels(true);
        fineToRoughSlider.setLabelTable(labelTable);
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

    private void initNoDuplicates() {
        noImageReuseCheckBox.setSelected(controller.isNoDuplicates());
        noImageReuseCheckBox.addItemListener(itemEvent -> controller.setNoDuplicates(noImageReuseCheckBox.isSelected()));
    }

    private void initMatcherOptions() {
        getButtonForMatcher(controller.getMatcherPreference()).setSelected(true);
        preferFormatMatcherRadioButton.addItemListener(itemEvent -> handleMatcherEvent(preferFormatMatcherRadioButton, EasyMosaicController.MatcherOption.RESOLUTION));
        preferColorMatcherRadioButton.addItemListener(itemEvent -> handleMatcherEvent(preferColorMatcherRadioButton, EasyMosaicController.MatcherOption.COLOR));
        chooseRandomlyRadioButton.addItemListener(itemEvent -> handleMatcherEvent(chooseRandomlyRadioButton, EasyMosaicController.MatcherOption.RANDOM));

    }

    private void handleMatcherEvent(JRadioButton button, EasyMosaicController.MatcherOption option) {
        if (button.isSelected()) {
            controller.setMatcherPreference(option);
        }
    }

    private JRadioButton getButtonForMatcher(EasyMosaicController.MatcherOption option) {
        switch (option) {
            case COLOR:
                return preferColorMatcherRadioButton;
            case RESOLUTION:
                return preferFormatMatcherRadioButton;
            case RANDOM:
                return chooseRandomlyRadioButton;
            default:
                throw new IllegalArgumentException("Option not supported:" + option);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Mosaic-chen");
        frame.setContentPane(new EasyMosaicGuide().contentPane);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }

    private void createUIComponents() {
    }

    private int getSourcesDirectoryIndex(SourcesDirectoryPanel pane) {
        int index = 0;
        for (Component component : sourcesDirectoryContainer.getComponents()) {
            if (pane.getContentPane() == component) {
                return index;
            }
            index++;
        }
        return -1;
    }

    @Override
    public void onSourcesDirectoryCreated(SourcesDirectory directory, int index) {
        System.out.println("Directory created:" + directory);
        final SourcesDirectoryPanel pane = new SourcesDirectoryPanel();
        sourcesDirectoryContainer.add(pane.getContentPane(), index);
        sourcesDiretoryPanes.add(index, pane);
        boolean loaded = directory.isLoaded();
        pane.getLoadingContent().setVisible(!loaded);
        pane.getLoadedContent().setVisible(loaded);
        updatePanelText(pane, directory);
        pane.getActiveCheckBox().addItemListener(itemEvent -> controller.setSourcesDirectoryActive(getSourcesDirectoryIndex(pane),
                                                                                                   itemEvent.getStateChange() == ItemEvent.SELECTED));
        updatePanelActiveState(pane, directory);
        pane.getDeleteButton().addActionListener(actionEvent -> controller.removeSourcesDirectory(getSourcesDirectoryIndex(pane)));

        pane.getCancelLoadingButton().addActionListener(actionEvent -> controller.cancelLoadDirectory(getSourcesDirectoryIndex(pane)));
        sourcesDirectoryContainer.revalidate();
        sourcesDirectoryContainer.repaint();
        if (!loaded) {
            pane.getLoadingBar().setToolTipText(directory.getDescription());
            controller.loadDirectory(index, progress -> pane.getLoadingBar().setValue(progress));
        }
    }

    private void updatePanelActiveState(SourcesDirectoryPanel pane, SourcesDirectory directory) {
        pane.getActiveCheckBox().setSelected(directory.isActive());
    }

    private void updatePanelText(SourcesDirectoryPanel pane, SourcesDirectory directory) {
        pane.getDescriptionText().setText(directory.getDescription());
    }

    private SourcesDirectoryPanel getSourceDirectoryPanel(int index) {
        return sourcesDiretoryPanes.get(index);
    }

    @Override
    public void onSourcesDirectoryStatusChange(SourcesDirectory directory, int index) {
        System.out.println("Directory changed status:" + directory);
        SourcesDirectoryPanel panel = getSourceDirectoryPanel(index);
        updatePanelText(panel, directory);
        updatePanelActiveState(panel, directory);
        if (directory.isLoaded()) {
            panel.getLoadingContent().setVisible(false);
            panel.getLoadedContent().setVisible(true);
        }
        sourcesDirectoryContainer.revalidate();
        sourcesDirectoryContainer.repaint();
    }

    @Override
    public void onSourcesDirectoryRemoved(SourcesDirectory directory, int previousIndex) {
        System.out.println("Directory removed:" + directory);
        sourcesDirectoryContainer.remove(previousIndex);
        sourcesDiretoryPanes.remove(previousIndex);
        sourcesDirectoryContainer.revalidate();
        sourcesDirectoryContainer.repaint();
    }
}
