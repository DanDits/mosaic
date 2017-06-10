package awt;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;

/**
 * Created by dd on 03.06.17.
 */
public class CanvasFactory extends AbstractCanvasFactory {
    @Override
    public AbstractCanvas makeCanvas(AbstractBitmap bitmap) {
        return new Canvas(bitmap);
    }
}
