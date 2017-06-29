package effects.workers;

import data.image.AbstractBitmap;
import effects.BitmapEffect;
import effects.SizeSupplier2D;

import java.util.Optional;

/**
 * Created by dd on 28.06.17.
 */
public class ResizeUsingDivisorsEffect implements BitmapEffect {
    private static final boolean DEFAULT_PREFER_SMALLER = true;
    private int wantedRows;
    private int wantedColumns;
    private SizeSupplier2D supplier;
    private boolean preferSmaller;
    private int targetWidth;
    private int targetHeight;

    public ResizeUsingDivisorsEffect(SizeSupplier2D resolutionSupplier) {
        this.supplier = resolutionSupplier;
        this.preferSmaller = DEFAULT_PREFER_SMALLER;
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
            columns = supplier.getColumns();
            rows = supplier.getRows();
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

    /**
     * Returns the closest value next to wantedCount which divides the given imageDimension.
     * @param imageDimension The dimension that must be divided. Must be positive. (Image height or width).
     * @param wantedCount The wanted amount of rows or columns.
     * @return A divisor of imageDimension (so in range 1 to imageDimension (inclusive)) which is closest
     * to wantedCount.
     * @throws IllegalArgumentException If a parameter is negative or zero.
     */
    public static int getClosestCount(int imageDimension, int wantedCount) {
        if (imageDimension <= 0 || wantedCount <= 0) {
            throw new IllegalArgumentException("Image and wanted rect dimension must be greater than zero");
        } else {
            if (wantedCount > imageDimension) {
                return imageDimension;
            } else {
                return findClosestDivisor(imageDimension, wantedCount);
            }
        }
    }

    /**
     * Returns the closest divisor to the given number. Not really fast, especially if
     * toNumber is prime. Expects valid parameters.
     * @param toNumber The number that has to be divided.
     * @param wantedDivisor The wanted divisior for the given number. Must be greater than zero and
     * smaller than or equal toNumber.
     * @return A proper divisor in range 1 to toNumber (both inclusive) of toNumber which
     * is closest to wantedDivisior;
     */
    private static int findClosestDivisor(int toNumber, int wantedDivisor) {
        int currDivisor = wantedDivisor;
        int delta = 0;
        while (toNumber % currDivisor != 0) {
            // makes the divisor greater/smaller by one each iteration, circulating near wantedDivisor
            // will terminate if divisor gets equal to one or to toNumber
            delta++;
            currDivisor = (delta % 2 == 0) ? (currDivisor - delta) : (currDivisor + delta);
        }
        return currDivisor;
    }
}
