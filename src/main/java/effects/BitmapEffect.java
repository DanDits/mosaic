package effects;

import data.image.AbstractBitmap;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Created by dd on 28.06.17.
 */
public interface BitmapEffect {
    Optional<AbstractBitmap> apply(AbstractBitmap source);

    default List<BitmapEffect> getPreEffects() {
        return Collections.emptyList();
    }

    default List<BitmapEffect> getPostEffects() {
        return Collections.emptyList();
    }
}
