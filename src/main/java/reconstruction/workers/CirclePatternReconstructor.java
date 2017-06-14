package reconstruction.workers;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import reconstruction.pattern.PatternReconstructor;
import reconstruction.pattern.PatternSource;
import util.image.ColorAnalysisUtil;
import util.image.ColorMetric;
import matching.TileMatcher;
import matching.workers.TrivialMatcher;

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
    }

    public static class Source<S> extends PatternSource<S> {
        private AbstractBitmap mPatternBitmap;
        private AbstractCanvas mCanvas;

        @Override
        protected AbstractBitmap makePattern(int color, AbstractBitmap base) {
            AbstractCanvas canvas = mCanvas;
            canvas.clear();
            canvas.drawCircle(base.getWidth() / 2, base.getHeight() / 2,
                    Math.min(base.getWidth() / 2, base.getHeight() / 2), color);
            return base;
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
    public <S> TileMatcher<S> makeMatcher(boolean useAlpha, ColorMetric metric) {
        return new TrivialMatcher<>();
    }

    @Override
    protected int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int endY) {
        ensureAverageBrightness(source);
        fillRaster(source, startX, endX, startY, endY);
        int width = endX - startX;
        int height = endY - startY;
        return calculateColor(mRaster, mAverageBrightness, width, height,
                0, 0, Math.max(width, height) + 1);
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

    public static int calculateColor(double[] raster, double averageBrightness,
                                     int bitmapWidth, int bitmapHeight,
                                     float x, float y, float r) {
        // by default this calculates the average brightness of the area [x-r,x+r][y-r,y+r]
        int left = (int)(x - r);
        int right = (int)(x + r);
        int top = (int)(y - r);
        int bottom = (int)(y + r);
        double brightness = 0;
        double consideredPoints = 0;
        // do not only consider pixels within the circle but within the square defined by the circle bounds
        for (int i = Math.max(0, left); i <= Math.min(right, bitmapWidth - 1); i++) {
            for (int j = Math.max(0, top); j <= Math.min(bottom, bitmapHeight - 1); j++) {
                int rasterIndex = j * bitmapWidth + i;
                if (rasterIndex >= 0 && rasterIndex < raster.length) {
                    brightness += raster[rasterIndex];
                    consideredPoints++;
                }
            }
        }
        // 1 = very bright -> white
        brightness /= consideredPoints;
        // logistic filter 1/(1+e^(-kx)) to minimize grey values and emphasize bright and dark ones
        // use higher k for less grey values
        brightness = 1. / (1. + Math.exp(-15. * (brightness - averageBrightness)));
        int grey = (int) (255. * brightness);
        return ColorAnalysisUtil.toRGB(grey, grey, grey, 255);
    }
}
