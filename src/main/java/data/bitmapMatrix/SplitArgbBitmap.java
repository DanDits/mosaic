package data.bitmapMatrix;


import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import util.image.Color;
import util.jama.Matrix;

/**
 * Created by daniel on 21.10.15.
 */
public class SplitArgbBitmap implements BitmapMatrix {
    private Matrix mMatrix;
    private boolean mTransposeRequired;

    public SplitArgbBitmap(AbstractBitmap bitmap) {
        mTransposeRequired = bitmap.getWidth() > bitmap.getHeight();
        final int width = Math.min(bitmap.getWidth() * 2, bitmap.getHeight() * 2);
        final int height = Math.max(bitmap.getWidth() * 2, bitmap.getHeight() * 2);
        Matrix matrix = new Matrix(height, width);
        for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
                final int color = bitmap.getPixel(mTransposeRequired ? i / 2 : j / 2,
                        mTransposeRequired ? j / 2 : i / 2);
                // if transpose is required, blue and green are swapped as they appear in uneven
                // rows or columns
                if (i % 2 == 0) {
                    // even row, here be red and green
                    if (j % 2 == 0) {
                        // red, even column
                        matrix.set(i, j, Color.red(color));
                    } else {
                        // green
                        matrix.set(i, j, mTransposeRequired ? Color.blue(color) : Color.green
                                (color));
                    }
                } else {
                    // uneven row, here be blue and alpha
                    if (j % 2 == 0) {
                        // blue, even column
                        matrix.set(i, j, mTransposeRequired ? Color.green(color) : Color.blue
                                (color));
                    } else {
                        matrix.set(i, j, Color.alpha(color));
                    }
                }
            }
        }
        mMatrix = matrix;
    }

    public SplitArgbBitmap(Matrix matrix) {
        mMatrix = matrix;
        if (matrix == null) {
            throw new IllegalArgumentException("No valid matrix given.");
        }
    }

    public AbstractBitmap convertToBitmap() {
        AbstractBitmap result = AbstractBitmapFactory.makeInstance(mMatrix.getColumnDimension() / 2, mMatrix
                .getRowDimension() / 2).createBitmap();

        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                int color = Color.argb(getAbstractColorValue(y * 2 + 1, x * 2 + 1),
                                       getAbstractColorValue(y * 2, x * 2),
                                       getAbstractColorValue(y * 2, x * 2 + 1),
                                       getAbstractColorValue(y * 2 + 1, x * 2));
                result.setPixel(x, y, color);
            }
        }
        return result;
    }

    @Override
    public boolean updateMatrix(Matrix matrix) {
        if (matrix == null) {
            return false;
        }
        if (mTransposeRequired) {
            matrix = matrix.transpose();
        }
        mMatrix = matrix;
        return true;
    }

    private int getAbstractColorValue(int row, int column) {
        int value = (int) mMatrix.get(row, column);
        return value < 255 ? (value > 0 ? value : 0) : 255;
    }

    public Matrix getMatrix() {
        return mMatrix;
    }

}
