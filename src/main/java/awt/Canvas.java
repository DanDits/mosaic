package awt;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import util.image.Color;
import data.image.PorterDuffMode;

import java.awt.*;
import java.awt.geom.QuadCurve2D;
import java.awt.image.BufferedImage;
import java.util.Deque;
import java.util.LinkedList;

/**
 * Created by dd on 03.06.17.
 */
public class Canvas implements AbstractCanvas {

    public static final BasicStroke ONE_PIXEL_STROKE = new BasicStroke(1);
    private final BufferedImage base;
    private final AbstractBitmap bitmap;
    private Graphics graphics;
    private static final java.awt.Color TRANSPARENT = new java.awt.Color(0, 0, 0, 0); // rgba

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

    @Override
    public void drawBitmap(AbstractBitmap bitmap, int x, int y, int fromBitmapX, int fromBitmapY, int toBitmapX, int toBitmapY) {
        ensureGraphics();
        BufferedImage image = obtainImage(bitmap);
        image = image.getSubimage(fromBitmapX, fromBitmapY, toBitmapX - fromBitmapX, toBitmapY - fromBitmapY);
        graphics.drawImage(image, x, y, null);
    }

    public BufferedImage stopEditing() {
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
        return base;
    }

    private java.awt.Color argbToColor(int argb) {
        return new java.awt.Color(Color.red(argb), Color.green(argb), Color.blue(argb),
                                  Color.alpha(argb));
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
    public void drawBitmapUsingPorterDuff(AbstractBitmap bitmap, int x, int y, int fromBitmapX, int fromBitmapY,
                                          int toBitmapX, int toBitmapY, PorterDuffMode mode) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        BufferedImage image = obtainImage(bitmap);
        Composite prevComp = graphics2D.getComposite();
        Composite comp = porterDuffToComposite(mode);
        graphics2D.setComposite(comp);
        image = image.getSubimage(fromBitmapX, fromBitmapY, toBitmapX - fromBitmapX, toBitmapY - fromBitmapY);
        graphics2D.drawImage(image, x, y, null);
        graphics2D.setComposite(prevComp);
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

    @Override
    public void drawLine(int fromX, int fromY, int toX, int toY, int color) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        java.awt.Color prevColor = graphics.getColor();
        graphics.setColor(argbToColor(color));
        Stroke prevStroke = graphics2D.getStroke();
        graphics2D.setStroke(ONE_PIXEL_STROKE);
        graphics.drawLine(fromX, fromY, toX, toY);
        graphics.setColor(prevColor);
        graphics2D.setStroke(prevStroke);
    }

    @Override
    public void floodFill(int x, int y, int color) {
        ensureGraphics();

        int originalColor = bitmap.getPixel(x, y);
        if (color == originalColor) {
            return;
        }
        Deque<Integer> stackX = new LinkedList<>();
        Deque<Integer> stackY = new LinkedList<>();
        stackX.add(x);
        stackY.add(y);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        while (!stackX.isEmpty()) {
            int currX = stackX.pop();
            int currY = stackY.pop();
            if (bitmap.getPixel(currX, currY) == originalColor) {
                bitmap.setPixel(currX, currY, color);
                attemptFloodToNeighbor(currX + 1, currY, stackX, stackY, width, height, originalColor);
                attemptFloodToNeighbor(currX - 1, currY, stackX, stackY, width, height, originalColor);
                attemptFloodToNeighbor(currX, currY + 1, stackX, stackY, width, height, originalColor);
                attemptFloodToNeighbor(currX, currY - 1, stackX, stackY, width, height, originalColor);
            }
        }
    }

    private void attemptFloodToNeighbor(int x, int y, Deque<Integer> stackX, Deque<Integer> stackY, int width, int height, int originalColor) {
        if (x < 0 || y < 0 || x >= width || y >= height) {
            return;
        }
        if (bitmap.getPixel(x, y) == originalColor) {
            stackX.add(x);
            stackY.add(y);
        }
    }

    @Override
    public void setAntiAliasing(boolean enable) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, enable ? RenderingHints.VALUE_ANTIALIAS_ON
                : RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    @Override
    public void drawQuadraticCurve(int fromX, int fromY, int overX, int overY, int toX, int toY, int color) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        java.awt.Color prevColor = graphics2D.getColor();
        graphics2D.setColor(argbToColor(color));
        QuadCurve2D q = new QuadCurve2D.Double();
        q.setCurve(fromX, fromY, overX, overY, toX, toY);
        graphics2D.draw(q);
        graphics2D.setColor(prevColor);
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
    public void drawMultiplicativly(AbstractBitmap bitmap, int x, int y) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        BufferedImage image = obtainImage(bitmap);
        Composite prevComp = graphics2D.getComposite();
        Composite comp = BlendComposite.Multiply;
        graphics2D.setComposite(comp);
        graphics2D.drawImage(image, x, y, null);
        graphics2D.setComposite(prevComp);
    }

    @Override
    public void drawMultiplicativly(AbstractBitmap bitmap, int x, int y, int fromBitmapX, int fromBitmapY, int toBitmapX, int toBitmapY) {
        ensureGraphics();
        Graphics2D graphics2D = (Graphics2D) graphics;
        BufferedImage image = obtainImage(bitmap);
        Composite prevComp = graphics2D.getComposite();
        Composite comp = BlendComposite.Multiply;
        graphics2D.setComposite(comp);
        image = image.getSubimage(fromBitmapX, fromBitmapY, toBitmapX - fromBitmapX, toBitmapY - fromBitmapY);
        graphics2D.drawImage(image, x, y, null);
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
