package reconstruction.pattern;


import data.AbstractBitmap;
import util.image.ColorMetric;
import matching.TileMatcher;
import reconstruction.RectReconstructor;

/**
 * Created by daniel on 05.12.15.
 */
public abstract class PatternReconstructor extends RectReconstructor {
    public PatternReconstructor(AbstractBitmap source, int wantedRows, int
            wantedColumns, int groundingColor) {
        super(source, wantedRows, wantedColumns);
        mResultCanvas.clear();
        mResultCanvas.drawColor(groundingColor);
    }

    // this is invoked by parent constructor, not best practice as subclass constructor not yet
    // initialized (missing members ...!)
    protected abstract int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int
            endY);

    public abstract <S> PatternSource<S> makeSource();

    public abstract <S> TileMatcher<S> makeMatcher(boolean useAlpha, ColorMetric metric);
}
