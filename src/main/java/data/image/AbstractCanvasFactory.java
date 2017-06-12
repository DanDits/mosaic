package data.image;

import awt.CanvasFactory;

/**
 * Created by dd on 03.06.17.
 */
public abstract class AbstractCanvasFactory {

    public static AbstractCanvasFactory getInstance() {
        return CanvasFactory.INSTANCE;
    }

    public abstract AbstractCanvas makeCanvas(AbstractBitmap bitmap);

    public AbstractCanvas makeCanvas(int width, int height) {
        AbstractBitmap bitmap = AbstractBitmapFactory.makeInstance(width, height).createBitmap();
        return makeCanvas(bitmap);
    }
}
