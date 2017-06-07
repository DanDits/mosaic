package awt;

import data.AbstractBitmap;
import data.AbstractCanvas;
import data.AbstractColor;
import data.PorterDuffMode;
import util.image.BlendComposite;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by dd on 03.06.17.
 */
public class Canvas implements AbstractCanvas {

    private final BufferedImage base;
    private final AbstractBitmap bitmap;
    private Graphics graphics;
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0); // rgba

    public Canvas(AbstractBitmap bitmap) {
        this.base = obtainImage(bitmap);
        this.bitmap = bitmap;
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

    public BufferedImage stopEditing() {
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

    public void drawBitmapUsingPorterDuff(AbstractBitmap bitmap, int x, int y, PorterDuffMode mode) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        BufferedImage image = obtainImage(bitmap);
        Composite prevComp = graphics2D.getComposite();
        Composite comp = porterDuffToComposite(mode);
        graphics2D.setComposite(comp);
        graphics2D.drawImage(image, x, y, null);
        graphics2D.setComposite(prevComp);
    }

    @Override
    public AbstractBitmap obtainImage() {
        stopEditing();
        return bitmap;
    }

    private static Composite porterDuffToComposite(PorterDuffMode mode) {
        int key;
        switch (mode) {
            case CLEAR:
                key = AlphaComposite.CLEAR;
                break;
            case DESTINATION:
                key = AlphaComposite.DST;
                break;
            case DESTINATION_IN:
                key = AlphaComposite.DST_IN;
                break;
            case DESTINATION_OUT:
                key = AlphaComposite.DST_OUT;
                break;
            case DESTINATION_OVER:
                key = AlphaComposite.DST_OVER;
                break;
            case DESTINATION_ATOP:
                key = AlphaComposite.DST_ATOP;
                break;
            case SOURCE:
                key = AlphaComposite.SRC;
                break;
            case SOURCE_ATOP:
                key = AlphaComposite.SRC_ATOP;
                break;
            case SOURCE_IN:
                key = AlphaComposite.SRC_IN;
                break;
            case SOURCE_OUT:
                key = AlphaComposite.SRC_OUT;
                break;
            case SOURCE_OVER:
                key = AlphaComposite.SRC_OVER;
                break;
            case XOR:
                key = AlphaComposite.XOR;
                break;
            default:
                throw new IllegalArgumentException("Unknown porter duff mode:" + mode);
        }
        return AlphaComposite.getInstance(key);
    }

    @Override
    public void drawMultiplicativly(AbstractBitmap bitmap) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        BufferedImage image = obtainImage(bitmap);
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
