package awt;

import data.AbstractBitmap;
import data.AbstractCanvas;
import data.AbstractCanvasFactory;

/**
 * Created by dd on 03.06.17.
 */
public class CanvasFactory extends AbstractCanvasFactory {
    @Override
    public AbstractCanvas makeCanvas(AbstractBitmap bitmap) {
        return new Canvas(bitmap);
    }
}
