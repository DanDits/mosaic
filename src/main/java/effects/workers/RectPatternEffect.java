package effects.workers;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import effects.BitmapEffect;
import effects.SizeSupplier2D;
import util.image.ColorAnalysisUtil;
import util.image.ColorSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Created by dd on 29.06.17.
 */
public abstract class RectPatternEffect implements BitmapEffect, SizeSupplier2D {

    private final int groundingColor;
    private final int wantedRows;
    private final int wantedColumns;
    private int rectHeight;
    private int rectWidth;
    private int[][] resultingARGB;
    protected final ColorSpace space;

    public RectPatternEffect(ColorSpace space, int wantedRows, int wantedColumns, int groundingColor) {
        Objects.requireNonNull(space);
        if (wantedColumns <= 0 || wantedRows <= 0) {
            throw new IllegalArgumentException("Illegal rows or columns.");
        }
        this.space = space;
        this.wantedRows = wantedRows;
        this.wantedColumns = wantedColumns;
        this.groundingColor = groundingColor;
    }

    @Override
    public int getRows() {
        return wantedRows;
    }

    @Override
    public int getColumns() {
        return wantedColumns;
    }

    @Override
    public final Optional<AbstractBitmap> apply(AbstractBitmap source) {
        analyze(source);
        return Optional.of(draw(source));
    }

    private AbstractBitmap draw(AbstractBitmap source) {
        AbstractCanvas canvas = AbstractCanvasFactory.getInstance().makeCanvas(source);
        canvas.clear();
        canvas.drawColor(groundingColor);
        for (int heightIndex = 0; heightIndex < resultingARGB.length; heightIndex++) {
            for (int widthIndex = 0; widthIndex < resultingARGB[heightIndex].length; widthIndex++) {
                drawRect(canvas, widthIndex * rectWidth, heightIndex * rectHeight,
                         (widthIndex + 1) * rectWidth, (heightIndex + 1) * rectHeight,
                         resultingARGB[heightIndex][widthIndex]);
            }
        }
        return canvas.obtainImage();
    }

    protected abstract void drawRect(AbstractCanvas canvas, int fromX, int fromY, int toX, int toY, int analyzedArgb);

    private void analyze(AbstractBitmap source) {
        int actualRows = ResizeUsingDivisorsEffect.getClosestCount(source.getHeight(), wantedRows);
        int actualColumns = ResizeUsingDivisorsEffect.getClosestCount(source.getWidth(), wantedColumns);
        rectHeight = source.getHeight() / actualRows;
        rectWidth = source.getWidth() / actualColumns;
        evaluateResultingArgb(source, actualRows, actualColumns);
    }

    private void evaluateResultingArgb(AbstractBitmap source, int rows, int columns) {
        resultingARGB = new int[rows][columns];
        for (int heightIndex = 0; heightIndex < rows; heightIndex++) {
            for (int widthIndex = 0; widthIndex < columns; widthIndex++) {
                resultingARGB[heightIndex][widthIndex]
                        = evaluateRectValue(source, widthIndex * rectWidth,
                                            (widthIndex + 1) * rectWidth,
                                            heightIndex * rectHeight,
                                            (heightIndex + 1) * rectHeight);
            }
        }
    }

    protected int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int endY) {
        return ColorAnalysisUtil.getAverageColor(source::getPixel, startX, endX, startY, endY, space);
    }

    @Override
    public List<BitmapEffect> getPreEffects() {
        List<BitmapEffect> effects = new ArrayList<>(1);
        effects.add(new ResizeUsingDivisorsEffect(this));
        return effects;
    }
}
