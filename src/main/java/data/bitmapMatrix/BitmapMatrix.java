package data.bitmapMatrix;

import data.image.AbstractBitmap;
import util.jama.Matrix;

/**
 * An interface to a holder of a matrix which represents in one way or another
 * a Bitmap object. The conversion to and from a bitmap is possible in both directions.
 * For compatibility and speed reasons the generated matrix is assured to have more rows
 * than columns. If this wouldn't have been the case usually, the method isMatrixTransposed()
 * will return true. Creating a BitmapMatrix container from a matrix should therefore transpose the
 * matrix again before handing it over.
 * Created by daniel on 22.10.15.
 */
public interface BitmapMatrix {
    AbstractBitmap convertToBitmap();
    boolean updateMatrix(Matrix matrix);
    Matrix getMatrix();
}
