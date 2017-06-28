package effects.workers;

import data.image.AbstractBitmap;
import data.image.ImageResolution;
import effects.BitmapEffect;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by dd on 28.06.17.
 */
public class ResizeUsingDivisorsEffect implements BitmapEffect {
    private static final boolean DEFAULT_PREFER_SMALLER = true;
    private int wantedRows;
    private int wantedColumns;
    private Supplier<ImageResolution> supplier;
    private boolean preferSmaller;
    private int targetWidth;
    private int targetHeight;

    public ResizeUsingDivisorsEffect(Supplier<ImageResolution> resolutionSupplier) {
        this.supplier = resolutionSupplier;
    }

    private ResizeUsingDivisorsEffect(int wantedColumns, int wantedRows) {
        this.wantedColumns = wantedColumns;
        this.wantedRows = wantedRows;
        this.preferSmaller = DEFAULT_PREFER_SMALLER;
    }

    public void setPreferSmaller(boolean preferSmaller) {
        this.preferSmaller = preferSmaller;
    }

    @Override
    public Optional<AbstractBitmap> apply(AbstractBitmap source) {
        targetWidth = source.getWidth();
        targetHeight = source.getHeight();
        int columns = wantedColumns;
        int rows = wantedRows;
        if (supplier != null) {
            ImageResolution resolution = supplier.get();
            columns = resolution.getWidth();
            rows = resolution.getHeight();
        }
        ensureTargetDimensionDivisibleBy(columns, rows, preferSmaller);
        if (source.getWidth() != targetWidth || source.getHeight() != targetHeight) {
            source.resize(targetWidth, targetHeight);
        }
        return Optional.of(source);
    }


    private void ensureTargetDimensionDivisibleBy(int widthDivisor, int heightDivisor, boolean preferSmaller) {
        // make sure that both dimension are divisible by the given divisor and greater than zero
        int widthDelta = -targetWidth;
        if (preferSmaller) {
            widthDelta = -(targetWidth % widthDivisor);
        }
        if (targetWidth + widthDelta <= 0) {
            widthDelta = widthDivisor + targetWidth % widthDivisor;
        }
        targetWidth += widthDelta;

        int heightDelta = -targetHeight;
        if (preferSmaller) {
            heightDelta = -(targetHeight % heightDivisor);
        }
        if (targetHeight + heightDelta <= 0) {
            heightDelta = heightDivisor + targetHeight % heightDivisor;
        }
        targetHeight += heightDelta;
    }
}
