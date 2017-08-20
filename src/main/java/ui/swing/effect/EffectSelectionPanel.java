package ui.swing.effect;

import ui.FileBitmapSource;
import ui.swing.creation.TileMatcherFactory;
import util.ProgressCallback;
import util.image.ColorSpace;

import javax.swing.*;

/**
 * Created by dd on 09.08.17.
 */
public class EffectSelectionPanel {
    private JPanel contentPane;
    private IconSpinner effectsSpinner;
    private JTextField name;
    private JTextField description;

    public EffectSelectionPanel() {
        effectsSpinner.init(EffectBuilderItem.makeAll(ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA,
                                                      new TileMatcherFactory(),
                                                      new FileBitmapSource(),
                                                      new ProgressCallback() {
                                                          @Override
                                                          public boolean isCancelled() {
                                                              return false;
                                                          }

                                                          @Override
                                                          public void onProgressUpdate(int progress) {
                                                              System.out.println("Progress!" + progress);
                                                          }
                                                      }));
        effectsSpinner.addChangeListener(changeEvent -> updateEffectDescription());
    }

    private void updateEffectDescription() {
        EffectBuilderItem effect = effectsSpinner.getSelectedEffect();
        name.setText(effect.getName());
        description.setText(effect.getDescription());
    }

    public JPanel getContentPane() {
        return contentPane;
    }
}
