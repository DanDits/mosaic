package awt;

import data.image.AbstractBitmap;
import net.coobird.thumbnailator.Thumbnails;
import org.pmw.tinylog.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;

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
    public AbstractBitmap getCopy() {
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), image.getType());
        Graphics graphics = copy.getGraphics();
        graphics.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        graphics.dispose();
        return new Bitmap(copy);
    }

    @Override
    public AbstractBitmap getRotatedCopy(double degree) {
        try {
            return new Bitmap(Thumbnails.of(image).scale(1.).rotate(degree).asBufferedImage());
        } catch (IOException e) {
            Logger.error("Some error rotating: {}", e); // should not happen was we do not load/save the image
            throw new IllegalArgumentException("Could not rotate.");
        }
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
        String type = "png";
        int dotIndex = file.getName().lastIndexOf(".");
        if (dotIndex >= 0) {
            switch (file.getName().substring(dotIndex).toLowerCase()) {
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
        }
        return ImageIO.write(image, type, file);
    }

    @Override
    public AbstractBitmap obtainResized(int width, int height) {
        return new Bitmap(makeResizedImage(width, height));
    }

    private BufferedImage makeResizedImage(int width, int height) {
        Image tmp = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        BufferedImage dimg;
        if (image.getType() == BufferedImage.TYPE_BYTE_INDEXED) {
            dimg = new BufferedImage(width, height, image.getType(), (IndexColorModel) image.getColorModel());
        } else {
            dimg = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        }
        Graphics2D g2d = dimg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();
        return dimg;
    }
}
