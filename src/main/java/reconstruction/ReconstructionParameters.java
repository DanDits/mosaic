package reconstruction;

import data.image.AbstractBitmap;
import effects.BitmapEffect;

import java.util.Collections;
import java.util.List;

/**
 * Created by daniel on 10.06.17.
 */
public abstract class ReconstructionParameters {
    public AbstractBitmap source;

    public static class IllegalParameterException extends Exception {
        public IllegalParameterException(Object parameter, String allowedDescription) {
            super("Parameter (" + parameter + ") not valid: " + allowedDescription);
        }
    }

    public ReconstructionParameters() {
        resetToDefaults();
    }

    public ReconstructionParameters setSourceBitmap(AbstractBitmap source) {
        this.source = source;
        return this;
    }

    public List<BitmapEffect> getPreReconstructionEffects() {
        return Collections.emptyList();
    }

    public List<BitmapEffect> getPostReconstructionEffects() {
        return Collections.emptyList();
    }

    public abstract Reconstructor makeReconstructor() throws IllegalParameterException;
    protected abstract void resetToDefaults();

    protected void validateParameters() throws IllegalParameterException {
        if (source == null) {
            throw new IllegalParameterException(null, "No source given.");
        }
    }
}
