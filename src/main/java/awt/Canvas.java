package awt;

import data.AbstractBitmap;
import data.AbstractCanvas;
import data.AbstractColor;
import util.image.BlendComposite;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by dd on 03.06.17.
 */
public class Canvas implements AbstractCanvas {

    private final BufferedImage base;
    private Graphics graphics;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0); // rgba

    public Canvas(AbstractBitmap base) {
        this.base = obtainImage(base);
    }


    private void ensureGraphics() {
        if (graphics == null) {
            graphics = base.getGraphics();
        }
    }

    @Override
    public void drawBitmap(AbstractBitmap bitmap, int x, int y) {
        ensureGraphics();
        graphics.drawImage(obtainImage(bitmap), x, y, null);
    }

    public BufferedImage getImage() {
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
        return base;
    }

    private Color argbToColor(int argb) {
        return new Color(AbstractColor.red(argb), AbstractColor.green(argb), AbstractColor.blue(argb),
                AbstractColor.alpha(argb));
    }

    @Override
    public void clear() {
        ensureGraphics();
        graphics.setColor(TRANSPARENT);
        graphics.fillRect(0, 0, base.getWidth(), base.getHeight());
    }

    @Override
    public void drawColor(int argb) {
        ensureGraphics();
        graphics.setColor(argbToColor(argb));
        graphics.fillRect(0, 0, base.getWidth(), base.getHeight());
    }

    @Override
    public void drawMultiplicativly(AbstractBitmap bitmap) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        BufferedImage image = obtainImage(bitmap);
        // reference (android): https://developer.android.com/reference/android/graphics/PorterDuff.Mode.html
        Composite prevComp = graphics2D.getComposite();
        Composite comp = BlendComposite.Multiply;
        graphics2D.setComposite(comp);
        graphics2D.drawImage(image, 0, 0, null);
        graphics2D.setComposite(prevComp);
    }

    @Override
    public void drawCircle(int centerX, int centerY, int radius, int color) {
        ensureGraphics();
        graphics.setColor(argbToColor(color));
        graphics.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
    }

    private static BufferedImage obtainImage(AbstractBitmap bitmap) {
        if (bitmap instanceof Bitmap) {
            // I could use type parameters to enforce the Canvas and Bitmap implementation match but this would
            // blow up the whole code for every usage of AbstractBitmap.
            // There will never be multiple implementations, the purpose of this setup is to have an easy way
            // to port the implementation to another framework (like android) which uses another graphics system
            return ((Bitmap) bitmap).getImage();
        }
        throw new IllegalArgumentException("Given bitmap does not match the canvas, cannot extract BufferedImage.");
    }
}
