package effects;

import data.image.AbstractBitmap;

import java.util.Optional;

/**
 * Created by dd on 28.06.17.
 */
@FunctionalInterface
public interface BitmapEffect {
    Optional<AbstractBitmap> apply(AbstractBitmap source);
}
