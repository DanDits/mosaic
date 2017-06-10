package bitmapMatrix;


import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.image.AbstractColor;
import util.jama.Matrix;

/**
 * Created by daniel on 22.10.15.
 */
public class SplitRgbBitmap implements BitmapMatrix {
    private Matrix mMatrix;
    private boolean mTransposeRequired;

    public SplitRgbBitmap(AbstractBitmap bitmap) {
        mTransposeRequired = bitmap.getWidth() > bitmap.getHeight() * 3;
        final int width = Math.min(bitmap.getWidth(), bitmap.getHeight() * 3);
        final int height = Math.max(bitmap.getWidth(), bitmap.getHeight() * 3);
        Matrix matrix = new Matrix(height, width);
        for (int i = 0; i < bitmap.getHeight(); i++) { //
            for (int j = 0; j < bitmap.getWidth(); j++) {
                final int color = bitmap.getPixel(j, i);
                if (mTransposeRequired) {
                    int column = i * 3;
                    matrix.set(j, column, AbstractColor.red(color));
                    matrix.set(j, column + 1, AbstractColor.green(color));
                    matrix.set(j, column + 2, AbstractColor.blue(color));
                } else {
                    int row = i * 3;
                    matrix.set(row, j, AbstractColor.red(color));
                    matrix.set(row + 1, j, AbstractColor.green(color));
                    matrix.set(row + 2, j, AbstractColor.blue(color));
                }
            }
        }
        mMatrix = matrix;
    }

    public SplitRgbBitmap(Matrix matrix) {
        mMatrix = matrix;
        if (matrix == null) {
            throw new IllegalArgumentException("No valid matrix given.");
        }
    }

    public AbstractBitmap convertToBitmap() {
        int width = mTransposeRequired ? mMatrix.getRowDimension() : mMatrix.getColumnDimension();
        int height = mTransposeRequired ? mMatrix.getColumnDimension() / 3 : mMatrix.getRowDimension() / 3;
        AbstractBitmap result = AbstractBitmapFactory.makeInstance(width, height).createBitmap();

        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                int color;
                if (mTransposeRequired) {
                    color = AbstractColor.argb(255,
                            getAbstractColorValue(y, x * 3),
                            getAbstractColorValue(y, x * 3 + 1),
                            getAbstractColorValue(y, x * 3 + 2));
                } else {
                    color = AbstractColor.argb(255,
                            getAbstractColorValue(y * 3, x),
                            getAbstractColorValue(y * 3 + 1, x),
                            getAbstractColorValue(y * 3 + 2, x));
                }
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
