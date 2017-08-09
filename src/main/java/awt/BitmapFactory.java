package awt;

import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import net.coobird.thumbnailator.Thumbnails;
import net.coobird.thumbnailator.tasks.UnsupportedFormatException;
import org.pmw.tinylog.Logger;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/**
 * Created by dd on 03.06.17.
 */
public class BitmapFactory extends AbstractBitmapFactory {
    private static final int DEFAULT_IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
    private int type;

    public BitmapFactory(int width, int height) {
        super(width, height);
        this.type = DEFAULT_IMAGE_TYPE;
    }

    public BitmapFactory(File file) {
        super(file);
        this.type = DEFAULT_IMAGE_TYPE;
    }

    public void setImageType(int type) {
        this.type = type;
    }

    @Override
    public AbstractBitmap createBitmap() {
        if (file != null) {
            try {
                BufferedImage image = Thumbnails.of(file).imageType(type).scale(1.).asBufferedImage();
                if (image == null) {
                    return null;
                }
                Logger.trace("Loaded image of type:" + image.getType());
                return new Bitmap(image);
            } catch (UnsupportedFormatException format) {
                Logger.trace("Not an image file: {}", file);
                return null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (width > 0 && height > 0) {
            return new Bitmap(new BufferedImage(width, height, type));
        }
        throw new IllegalStateException("Cannot create bitmap from given data:" + file + "width/height=" + width + "/" + height);
    }






}
