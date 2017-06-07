package reconstruction;

import data.AbstractBitmap;
import data.AbstractCanvas;
import data.AbstractCanvasFactory;

/**
 * Created by dd on 07.06.17.
 */
public class PuzzleReconstructor extends Reconstructor {

    private final int mRectHeight;
    private final int mRectWidth;
    private final AbstractCanvas mResultCanvas;
    private AbstractBitmap result;

    public PuzzleReconstructor(AbstractBitmap source, int wantedRows, int wantedColumns, double originalImagesFraction) {
        if (source == null) {
            throw new NullPointerException();
        }
        if (wantedRows <= 0 || wantedColumns <= 0) {
            throw new IllegalArgumentException("An image cannot be reconstructed to zero rows or columns.");
        }
        int actualRows = Reconstructor.getClosestCount(source.getHeight(), wantedRows);
        int actualColumns = Reconstructor.getClosestCount(source.getWidth(), wantedColumns);
        this.mRectHeight = source.getHeight() / actualRows;
        this.mRectWidth = source.getWidth() / actualColumns;
        this.result = obtainBaseBitmap(this.mRectWidth * actualColumns, this.mRectHeight * actualRows);
        mResultCanvas = AbstractCanvasFactory.getInstance().makeCanvas(result);
    }

    @Override
    public boolean giveNext(AbstractBitmap nextFragmentImage) {
        return false;
    }

    @Override
    public MosaicFragment nextFragment() {
        return null;
    }

    @Override
    public boolean hasAll() {
        return false;
    }

    @Override
    public AbstractBitmap getReconstructed() {
        return result;
    }

    @Override
    public int estimatedProgressPercent() {
        return 0;
    }
}
