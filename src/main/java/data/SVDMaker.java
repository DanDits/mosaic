package data;

import bitmapMatrix.ARGBMatrix;
import bitmapMatrix.BitmapMatrix;
import bitmapMatrix.IndexedBitmap;
import bitmapMatrix.SplitArgbBitmap;
import data.AbstractBitmap;
import util.PercentProgressListener;
import util.jama.Matrix;
import util.jama.SingularValueDecomposition;
import bitmapMatrix.SplitRgbBitmap;
import data.MosaicMaker;

/**
 * Created by daniel on 17.10.15.
 */
public class SVDMaker {
    public static final int MODE_ARGB_BITMAP = 1;
    public static final int MODE_INDEXED_BITMAP = 2;
    public static final int MODE_ARGB_SPLIT = 3;
    public static final int MODE_RGB_SPLIT = 4;
    private final double[] mSingularValues;
    private final Matrix mU;
    private final Matrix mVTransposed;
    private BitmapMatrix mBitmapMatrix;
    private int mMode;
    private final int mRank;

    public SVDMaker(AbstractBitmap base, int mode, final MosaicMaker.ProgressCallback
            callback) {
        mMode = mode;
        Matrix sourceMatrix;
        callback.onProgressUpdate(5);
        switch (mMode) {
            case MODE_INDEXED_BITMAP:
                mBitmapMatrix = new IndexedBitmap(base);
                break;
            case MODE_ARGB_SPLIT:
                mBitmapMatrix = new SplitArgbBitmap(base);
                break;
            case MODE_RGB_SPLIT:
                mBitmapMatrix = new SplitRgbBitmap(base);
                break;
            default:
                mMode = MODE_ARGB_BITMAP;
                // fall through
            case MODE_ARGB_BITMAP:
                mBitmapMatrix = new ARGBMatrix(base);
                break;
        }
        sourceMatrix = mBitmapMatrix.getMatrix();
        callback.onProgressUpdate(25);

        SingularValueDecomposition decomposition = new SingularValueDecomposition(sourceMatrix,
                new SingularValueDecomposition.ProgressCallback() {

                    @Override
                    public boolean isCancelled() {
                        return callback.isCancelled();
                    }

                    @Override
                    public void onProgressUpdate(int progress) {
                        callback.onProgressUpdate(25 + (int) (progress / (double)
                                PercentProgressListener
                                        .PROGRESS_COMPLETE * 55));
                    }
                });
        callback.onProgressUpdate(80);
        mRank = decomposition.rank();
        mSingularValues = decomposition.getSingularValues();
        mU = decomposition.getU();
        mVTransposed = decomposition.getV().transpose();
    }

    public int getMaxRank() {
        return mRank;
    }

    public AbstractBitmap getRankApproximation(int rank) {

        Matrix resultMatrix = mU.diagTimes(mSingularValues, rank, mVTransposed);

        mBitmapMatrix.updateMatrix(resultMatrix);
        return mBitmapMatrix.convertToBitmap();
    }
}
