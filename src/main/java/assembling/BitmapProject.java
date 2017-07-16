package assembling;

import data.export.AbstractBitmapExporter;
import data.export.BitmapExportException;
import data.image.AbstractBitmap;
import effects.BitmapEffect;
import effects.EffectGroup;

import java.util.Objects;
import java.util.Optional;

/**
 * Created by dd on 29.06.17.
 */
public class BitmapProject {
    private final EffectGroup rootGroup;
    private final AbstractBitmapExporter exporter;

    public BitmapProject(AbstractBitmapExporter exporter) {
        Objects.requireNonNull(exporter);
        rootGroup = EffectGroup.createEmpty();
        this.exporter = exporter;
    }

    public BitmapProject(BitmapEffect effect, AbstractBitmapExporter exporter) {
        this(exporter);
        addEffect(effect);
    }

    public void addEffect(BitmapEffect effect) {
        rootGroup.addEffect(effect);
    }

    public void build(AbstractBitmap source) throws BitmapExportException {
        Optional<AbstractBitmap> result = rootGroup.apply(source);
        if (result.isPresent()) {
            exporter.exportBitmap(result.get());
        }
    }
}
