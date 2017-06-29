package effects;

import data.image.AbstractBitmap;
import org.pmw.tinylog.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by dd on 29.06.17.
 */
public class EffectGroup implements BitmapEffect {
    private List<BitmapEffect> effects;

    private EffectGroup() {
        this.effects = new ArrayList<>();
    }

    public void addEffect(BitmapEffect effect) {
        if (effect == null) {
            return;
        }
        effects.add(effect);
    }

    private void addEffects(List<BitmapEffect> newEffects) {
        if (newEffects == null) {
            return;
        }
        for (BitmapEffect effect : newEffects) {
            addEffect(effect);
        }
    }

    public static EffectGroup createUsingPreAndPostEffects(BitmapEffect actualEffect) {
        Objects.requireNonNull(actualEffect);
        EffectGroup group = new EffectGroup();
        group.addEffects(actualEffect.getPreEffects());
        group.addEffect(actualEffect);
        group.addEffects(actualEffect.getPostEffects());
        return group;
    }

    @Override
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
                Logger.error("EffectGroup. Effect number {}/{} failed on given source image. Abort.", i + 1,
                             effects.size());
                return Optional.empty();
            }
            current = result.get();
            i++;
        }
        return Optional.of(current);
    }

    public static EffectGroup createEmpty() {
        return new EffectGroup();
    }
}
