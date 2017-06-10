package data.image;

import awt.CanvasFactory;

/**
 * Created by dd on 03.06.17.
 */
public abstract class AbstractCanvasFactory {

    private static final CanvasFactory INSTANCE = new CanvasFactory();

    public static AbstractCanvasFactory getInstance() {
        return INSTANCE;
    }

    public abstract AbstractCanvas makeCanvas(AbstractBitmap bitmap);
}
