package assembling;

import data.image.AbstractBitmap;
import effects.BitmapEffect;
import effects.EffectGroup;

import java.util.Optional;

/**
 * Created by dd on 29.06.17.
 */
public class BitmapProject {
    private final EffectGroup rootGroup;

    public BitmapProject() {
        rootGroup = EffectGroup.createEmpty();
    }

    public BitmapProject(BitmapEffect effect) {
        this();
        addEffect(effect);
    }

    public void addEffect(BitmapEffect effect) {
        rootGroup.addEffect(effect);
    }

    public Optional<AbstractBitmap> build(AbstractBitmap source) {
        return rootGroup.apply(source);
    }
}
