package ui.swing.main;

import ui.swing.sourcesdir.SourcesPanel;

import javax.swing.*;
import java.awt.*;
import java.util.Hashtable;

/**
 * Created by dd on 29.06.17.
 */
public class EasyMosaicGuide {
    private EasyMosaicController controller;
    private JPanel mosaic_file_panel;
    private JPanel reconstructor_panel;
    private JTextField mosaic_main_info_text;
    private JPanel sourcesContainer;
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
    private SourcesPanel sourcesPanel;

    private EasyMosaicGuide() {
        controller = new EasyMosaicController();
        addSimpleBorder(options);
        addSimpleBorder(sourcesContainer);
        addSimpleBorder(main_effect_panel);

        initNoDuplicates();
        initMatcherOptions();

        initFineRoughSlider();
        mosaic_main_info_text.setFont(new Font(mosaic_main_info_text.getFont().getFontName(), Font.BOLD, 25));
        sourcesPanel = new SourcesPanel();
        sourcesContainer.setLayout(new GridLayout(1, 1));
        sourcesContainer.add(sourcesPanel.getContentPane());
    }

    private static void addSimpleBorder(JComponent comp) {
        int width = 2;
        comp.setBorder(BorderFactory.createMatteBorder(width, width, width, width, Color.BLACK));
    }

    private void initFineRoughSlider() {
        Hashtable<Integer, JLabel> labelTable = new Hashtable<>();
        labelTable.put(0, new JLabel("Fein"));
        labelTable.put( 100, new JLabel("Grob"));

        fineToRoughSlider.setPaintLabels(true);
        fineToRoughSlider.setLabelTable(labelTable);
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

}
