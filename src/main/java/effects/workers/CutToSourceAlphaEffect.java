package effects.workers;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import data.image.PorterDuffMode;
import effects.BitmapEffect;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by dd on 28.06.17.
 */
public class CutToSourceAlphaEffect implements BitmapEffect {

    private final AbstractBitmap original;

    public CutToSourceAlphaEffect(AbstractBitmap original) {
        Objects.requireNonNull(original);
        this.original = original;
    }

    @Override
    public Optional<AbstractBitmap> apply(AbstractBitmap source) {
        AbstractCanvas canvas = AbstractCanvasFactory.getInstance().makeCanvas(source);
        canvas.drawBitmapUsingPorterDuff(original, 0, 0, PorterDuffMode.DESTINATION_IN);
        return Optional.ofNullable(canvas.obtainImage());
    }
}
