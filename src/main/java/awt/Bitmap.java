package awt;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import data.image.AbstractBitmap;

import javax.imageio.ImageIO;

/**
 * Created by dd on 03.06.17.
 */
public class Bitmap implements AbstractBitmap {
    private BufferedImage image;

    public Bitmap(BufferedImage image) {
        this.image = image;
        if (image == null) {
            throw new NullPointerException("Image null.");
        }
    }

    BufferedImage getImage() {
        return image;
    }

    @Override
    public int getWidth() {
        return image.getWidth();
    }

    @Override
    public int getHeight() {
        return image.getHeight();
    }

    @Override
    public int getPixel(int x, int y) {
        return image.getRGB(x, y);
    }

    @Override
    public void setPixel(int x, int y, int value) {
        image.setRGB(x, y, value);
    }

    @Override
    public void resize(int width, int height) {
        if (width == image.getWidth() && height == image.getHeight()) {
            return;
        }
        image = makeResizedImage(width, height);
    }

    @Override
    public boolean saveToFile(File file) throws IOException {
        String type;
        switch (file.getName().toLowerCase()) {
            case "jpg":
            case "jpeg":
                type = "jpg";
                break;
            case "gif":
                type = "gif";
                break;
            default:
            case "png":
                type = "png";
                break;
        }
        return ImageIO.write(image, type, file);
    }

    @Override
    public AbstractBitmap obtainResized(int width, int height) {
        return new Bitmap(makeResizedImage(width, height));
    }

    private BufferedImage makeResizedImage(int width, int height) {
        Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return dimg;
    }
}
