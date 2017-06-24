package reconstruction.workers;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import matching.TileMatcher;
import matching.workers.TrivialMatcher;
import reconstruction.ReconstructionParameters;
import reconstruction.pattern.PatternReconstructor;
import reconstruction.pattern.PatternSource;
import util.image.Color;
import util.image.ColorAnalysisUtil;
import util.image.ColorSpace;

/**
 * Created by daniel on 05.12.15.
 */
public class CirclePatternReconstructor extends PatternReconstructor {
    public static final String NAME = "Circles";
    private double[] mRaster;
    private double mAverageBrightness;

    public static class CircleParameters extends PatternParameters {

        public CircleParameters(AbstractBitmap source) {
            super(source);
        }

        @Override
        public PatternReconstructor makeReconstructor() throws IllegalParameterException {
            return new CirclePatternReconstructor(this);
        }

        @Override
        public ColorSpace getColorSpace(ColorSpace defaultSpace) {
            return ColorSpace.Brightness.INSTANCE_WITH_ALPHA;
        }
    }

    public static class Source<S> extends PatternSource<S> {
        private AbstractBitmap mPatternBitmap;
        private AbstractCanvas mCanvas;

        @Override
        protected AbstractBitmap makePattern(int color, AbstractBitmap base) {
            mCanvas.drawCircle(base.getWidth() / 2, base.getHeight() / 2,
                    Math.min(base.getWidth() / 2, base.getHeight() / 2), color);
            return mPatternBitmap;
        }

        protected AbstractBitmap obtainBitmap(int key, int width, int height) {
            if (mPatternBitmap != null && mPatternBitmap.getWidth() == width && mPatternBitmap
                    .getHeight() == height) {
                return mPatternBitmap;
            }
            mPatternBitmap = super.obtainBitmap(key, width, height);
            mCanvas = AbstractCanvasFactory.getInstance().makeCanvas(mPatternBitmap);
            return mPatternBitmap;
        }
    }

    public CirclePatternReconstructor(CircleParameters parameters) throws ReconstructionParameters.IllegalParameterException {
        super(parameters);
    }

    public <S> PatternSource<S> makeSource() {
        return new Source<>();
    }

    @Override
    public <S> TileMatcher<S> makeMatcher(ColorSpace space) {
        return new TrivialMatcher<>();
    }

    @Override
    protected int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int endY) {
        ensureAverageBrightness(source);
        fillRaster(source, startX, endX, startY, endY);
        return calculateColor(mRaster, mAverageBrightness);
    }

    private void fillRaster(AbstractBitmap source, int startX, int endX, int startY, int endY) {
        int width = endX - startX;
        int height = endY - startY;
        if (mRaster == null || mRaster.length != width * height) {
            mRaster = new double[width * height];
        }
        int index = 0;
        for (int j = startY; j < endY; j++) {
            for (int i = startX; i < endX; i++) {
                mRaster[index++] = ColorAnalysisUtil.getBrightnessWithAlpha(source.getPixel(i, j));
            }
        }
    }

    private void ensureAverageBrightness(AbstractBitmap source) {
        if (mAverageBrightness > 0.) {
            return;
        }
        mAverageBrightness = 0.;
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                mAverageBrightness += ColorAnalysisUtil.getBrightnessWithAlpha(source.getPixel(x,
                        y));
            }
        }
        mAverageBrightness /= source.getWidth() * source.getHeight();
        mAverageBrightness = Math.max(mAverageBrightness, Double.MIN_VALUE); // to ensure it is
        // positive and brightness not calculated over and over if it would be exactly zero
    }

    private static int calculateColor(double[] raster, double averageBrightness) {
        double brightness = 0;
        double consideredPoints = raster.length;
        // do not only consider pixels within the circle but within the square defined by the circle bounds
        for (double aRaster : raster) {
            brightness += aRaster;
        }
        // 1 = very bright -> white
        brightness /= consideredPoints;
        // logistic filter 1/(1+e^(-kx)) to minimize grey values and emphasize bright and dark ones
        // use higher k for less grey values
        brightness = 1. / (1. + Math.exp(-5. * (brightness - averageBrightness)));
        int grey = (int) (255. * brightness);
        return Color.rgb(grey, grey, grey);
    }
}
