package awt;

import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import net.coobird.thumbnailator.Thumbnails;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;


/**
 * Created by dd on 03.06.17.
 */
public class BitmapFactory extends AbstractBitmapFactory {
    public static final int DEFAULT_IMAGE_TYPE = BufferedImage.TYPE_INT_ARGB;
    private int type;

    public BitmapFactory(int width, int height) {
        super(width, height);
        this.type = DEFAULT_IMAGE_TYPE;
    }

    public BitmapFactory(File file) {
        super(file);
    }

    public void setImageType(int type) {
        this.type = type;
    }

    @Override
    public AbstractBitmap createBitmap() {
        if (file != null) {
            try {
                BufferedImage image = Thumbnails.of(file).scale(1.).asBufferedImage();
                if (image == null) {
                    return null;
                }
                return new Bitmap(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (width > 0 && height > 0) {
            return new Bitmap(new BufferedImage(width, height, type));
        }
        throw new IllegalStateException("Cannot create bitmap from given data:" + file + "width/height=" + width + "/" + height);
    }






}
