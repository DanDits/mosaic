package effects.workers;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import util.image.ColorAnalysisUtil;
import util.image.ColorSpace;

/**
 * Created by dd on 28.06.17.
 */
public class CirclesEffect extends RectPatternEffect {

    private double totalAverageBrightness = -1.;

    public CirclesEffect(ColorSpace space, int wantedRows, int wantedColumns, int groundingColor) {
        super(space, wantedRows, wantedColumns, groundingColor);
    }

    @Override
    protected void drawRect(AbstractCanvas canvas, int fromX, int fromY, int toX, int toY, int analyzedArgb) {
        int width = toX - fromX;
        int height = toY - fromY;
        canvas.drawCircle(fromX + width / 2, fromY + height / 2,
                           Math.min(width / 2, height / 2), analyzedArgb);
    }

    @Override
    protected int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int endY) {
        double[] values = ColorAnalysisUtil.getAverageValues(source::getPixel, startX, endX, startY, endY, space);
        if (space.equals(ColorSpace.Brightness.INSTANCE_WITH_ALPHA)
                || space.equals(ColorSpace.Brightness.INSTANCE_WITHOUT_ALPHA)) {
            assert values.length == 1;
            ensureTotalAverageBrightness(source);
            // logistic filter 1/(1+e^(-kx)) to minimize grey values and emphasize bright and dark ones
            // use higher k for less grey values
            double brightness = 1. / (1. + Math.exp(-5. * (values[0] - totalAverageBrightness)));
            values[0] = brightness;
        }
        return space.valuesToArgb(values);
    }

    private void ensureTotalAverageBrightness(AbstractBitmap source) {
        if (totalAverageBrightness == -1.) {
            totalAverageBrightness = ColorAnalysisUtil.getAverageValues(source::getPixel, 0, source.getWidth(),
                                                                        0, source.getHeight(), space)[0];
        }
    }
}
