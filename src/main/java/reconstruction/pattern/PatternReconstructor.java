package reconstruction.pattern;


import data.image.AbstractBitmap;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import util.image.ColorMetric;
import matching.TileMatcher;
import reconstruction.workers.RectReconstructor;

/**
 * Created by daniel on 05.12.15.
 */
public abstract class PatternReconstructor extends RectReconstructor {

    public static abstract class PatternParameters extends RectReconstructor.RectParameters {
        public int groundingColor;
        public PatternParameters(AbstractBitmap source) {
            super(source);
        }
        @Override
        protected void resetToDefaults() {
            super.resetToDefaults();
            groundingColor = 0xFF000000;
        }

        public abstract PatternReconstructor makeReconstructor() throws IllegalParameterException;

        public abstract ColorMetric getColorMetric(ColorMetric colorMetric);
    }

    public PatternReconstructor(PatternParameters parameters) throws ReconstructionParameters.IllegalParameterException {
        super(parameters);
        mResultCanvas.clear();
        mResultCanvas.drawColor(parameters.groundingColor);
    }

    // this is invoked by parent constructor, not best practice as subclass constructor not yet
    // initialized (missing members ...!)
    protected abstract int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int
            endY);

    public abstract <S> PatternSource<S> makeSource();

    public abstract <S> TileMatcher<S> makeMatcher(boolean useAlpha, ColorMetric metric);
}
