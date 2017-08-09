package ui.swing.effect;

import net.coobird.thumbnailator.Thumbnails;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;

/**
 * Created by dd on 09.08.17.
 */
public class EffectSelectionPanel {
    private JPanel contentPane;
    private IconSpinner iconSpinner1;

    public EffectSelectionPanel() {
        URL resource = EffectSelectionPanel.class.getResource("icon_lego.png");
        BufferedImage image = null;
        try {
            image = Thumbnails.of(resource).size(50, 50).asBufferedImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
        iconSpinner1.setIcons(new Icon[] {new ImageIcon(image)});
    }
    public JPanel getContentPane() {
        return contentPane;
    }
}
