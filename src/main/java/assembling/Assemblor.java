package assembling;

import data.image.AbstractBitmap;
import effects.BitmapEffect;
import org.pmw.tinylog.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Created by dd on 28.06.17.
 */
public class Assemblor {

    private List<BitmapEffect> effects;

    public Assemblor() {
        effects = new LinkedList<>();
    }

    public void addEffect(BitmapEffect effect) {
        if (effect != null) {
            effects.add(effect);
        }
    }

    public Optional<AbstractBitmap> apply(AbstractBitmap source) {
        if (source == null) {
            return Optional.empty();
        }
        if (effects.isEmpty()) {
            return Optional.of(source);
        }
        AbstractBitmap current = source;
        int i = 0;
        for (BitmapEffect effect : effects) {
            Optional<AbstractBitmap> result = effect.apply(current);
            if (!result.isPresent()) {
                Logger.error("Assemblor. Effect number {}/{} failed on given source image. Abort.", i + 1,
                             effects.size());
                return null;
            }
            current = result.get();
            i++;
        }
        return Optional.of(current);
    }
}
