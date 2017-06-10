package reconstruction;

import data.image.AbstractBitmap;

/**
 * Created by daniel on 10.06.17.
 */
public abstract class ReconstructionParameters {
    protected final AbstractBitmap source;

    public AbstractBitmap getBitmapSource() {
        return source;
    }

    public static class IllegalParameterException extends Exception {
        public IllegalParameterException(Object parameter, String allowedDescription) {
            super("Parameter (" + parameter + ") not valid: " + allowedDescription);
        }
    }

    public ReconstructionParameters(AbstractBitmap source) {
        this.source = source;
        if (source == null) {
            throw new NullPointerException("Given source image is null.");
        }
        resetToDefaults();
    }

    public abstract Reconstructor makeReconstructor() throws IllegalParameterException;
    protected abstract void resetToDefaults();
    protected abstract void validateParameters() throws IllegalParameterException;
}
