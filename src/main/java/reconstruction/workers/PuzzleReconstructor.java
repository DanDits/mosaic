package reconstruction.workers;

import data.image.AbstractBitmap;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import reconstruction.MosaicFragment;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import util.image.ColorAnalysisUtil;

import java.util.Random;

/**
 * Created by dd on 07.06.17.
 */
public class PuzzleReconstructor extends Reconstructor {

    private final int mRectHeight;
    private final int mRectWidth;
    private final AbstractCanvas mResultCanvas;
    private final int columns;
    private final int rows;
    private AbstractBitmap source;
    private AbstractBitmap result;
    private Random random;
    private int currentX;
    private int currentY;
    private PuzzlePiece[] prevRow;
    private PuzzlePiece[] currentRow;
    private MosaicFragment wantedFragment;

    public static class PuzzleParameters extends ReconstructionParameters {
        public static final double MINIMUM_FRACTION = 0.1;
        public int wantedRows;
        public int wantedColumns;

        public PuzzleParameters(AbstractBitmap source) {
            super(source);
        }

        @Override
        public Reconstructor makeReconstructor() throws IllegalParameterException {
            return new PuzzleReconstructor(this);
        }

        @Override
        protected void resetToDefaults() {
            wantedRows = wantedColumns = 20;
        }

        @Override
        protected void validateParameters() throws IllegalParameterException {
            if (wantedRows <= 0) {
                throw new IllegalParameterException(wantedRows, "Rows must be positive.");
            }
            if (wantedColumns <= 0) {
                throw new IllegalParameterException(wantedColumns, "Columns must be positive.");
            }
        }
    }

    public PuzzleReconstructor(PuzzleParameters parameters) throws ReconstructionParameters.IllegalParameterException {
        parameters.validateParameters();
        this.source = parameters.getBitmapSource();
        int actualRows = Reconstructor.getClosestCount(source.getHeight(), parameters.wantedRows);
        int actualColumns = Reconstructor.getClosestCount(source.getWidth(), parameters.wantedColumns);
        this.rows = actualRows;
        this.columns = actualColumns;
        this.mRectHeight = source.getHeight() / actualRows;
        this.mRectWidth = source.getWidth() / actualColumns;
        random = new Random();
        this.result = obtainBaseBitmap(this.mRectWidth * actualColumns, this.mRectHeight * actualRows);
        mResultCanvas = AbstractCanvasFactory.getInstance().makeCanvas(result);
    }

    private class PuzzlePiece {
        private final PuzzlePiece leftPiece;
        private final PuzzlePiece upperPiece;
        private final int posY;
        private final int posX;
        private int genderUp, genderLeft, genderRight, genderDown;
        private AbstractBitmap bitmap;

        public PuzzlePiece(int posX, int posY, PuzzlePiece upperPiece, PuzzlePiece leftPiece) {
            this.posX = posX;
            this.posY = posY;
            this.upperPiece = upperPiece;
            this.leftPiece = leftPiece;
            genderUp = upperPiece == null ? 0 : -upperPiece.genderDown;
            genderLeft = leftPiece == null ? 0 : -leftPiece.genderRight;
            genderRight = posX == columns - 1 ? 0 : getNewGender();
            genderDown = posY == rows - 1 ? 0 : getNewGender();
        }

        private int getRequiredWidth() {
            return mRectWidth + (genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0)
                    + (genderRight == 1 ? getPuzzleHorizontalNoseLength() : 0);
        }

        private int getRequiredHeight() {
            return mRectHeight + (genderUp == 1 ? getPuzzleVerticalNoseLength() : 0)
                    + (genderDown == 1 ? getPuzzleVerticalNoseLength() : 0);
        }

        public int getAverageColor() {
            int startX = posX * mRectWidth - (genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0);
            int startY = posY * mRectHeight - (genderUp == 1 ? getPuzzleVerticalNoseLength() : 0);
            int endX = (posX + 1) * mRectWidth + (genderRight == 1 ? getPuzzleHorizontalNoseLength() : 0);
            int endY = (posY + 1) * mRectHeight + (genderDown == 1 ? getPuzzleVerticalNoseLength() : 0);
            return ColorAnalysisUtil.getAverageColor(source, startX, endX, startY, endY);
        }

        public void draw(AbstractBitmap bitmap) {
            this.bitmap = bitmap;
            drawInner();
            drawUpper();
            drawLeft();
        }

        private void drawLeft() {
            if (genderLeft == -1) {
                leftPiece.drawRight();
            }
        }

        private void drawRight() {

        }

        public void drawInner() {
            int startX = genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0;
            int startY = genderUp == 1 ? getPuzzleVerticalNoseLength() : 0;
            int endX = genderRight == 1 ? bitmap.getWidth() - getPuzzleHorizontalNoseLength() : bitmap.getWidth();
            int endY = genderDown == 1 ? bitmap.getHeight() - getPuzzleVerticalNoseLength() : bitmap.getHeight();
            mResultCanvas.drawBitmap(bitmap, posX * mRectWidth, posY * mRectHeight, startX, startY, endX, endY);
        }

        public void drawUpper() {
            if (genderUp == -1) {
                upperPiece.drawLower();
            }
        }

        public void drawLower() {

        }

    }

    private int getNewGender() {
        return random.nextBoolean() ? 1 : -1;
    }

    private int getPuzzleHorizontalNoseLength() {
        return (int) (mRectWidth * 0.2);
    }

    private int getPuzzleVerticalNoseLength() {
        return (int) (mRectHeight * 0.2);
    }

    @Override
    public boolean giveNext(AbstractBitmap nextFragmentImage) {
        if (hasAll()) {
            return false;
        }
        if (nextFragmentImage == null || nextFragmentImage.getWidth() != wantedFragment.getWidth()
                || nextFragmentImage.getHeight() != wantedFragment.getHeight()) {
            return false;
        }
        currentRow[currentX].draw(nextFragmentImage);
        advanceIndex();
        wantedFragment = null;
        return true;
    }

    private void advanceIndex() {
        currentX++;
        if (currentX == columns) {
            currentX = 0;
            currentY++;
            prevRow = currentRow;
            currentRow = null;
        }
    }

    @Override
    public MosaicFragment nextFragment() {
        if (wantedFragment != null) {
            return wantedFragment;
        }
        if (hasAll()) {
            return null;
        }
        if (currentRow == null) {
            currentRow = new PuzzlePiece[columns];
        }
        PuzzlePiece currentPiece = new PuzzlePiece(currentX, currentY, prevRow == null ? null : prevRow[currentX],
                currentX == 0 ? null : currentRow[currentX - 1]);
        currentRow[currentX] = currentPiece;
        wantedFragment = new MosaicFragment(currentPiece.getRequiredWidth(), currentPiece.getRequiredHeight(),
                currentPiece.getAverageColor());
        return wantedFragment;
    }

    @Override
    public boolean hasAll() {
        return currentX >= columns || currentY >= rows;
    }

    @Override
    public AbstractBitmap getReconstructed() {
        return result;
    }

    @Override
    public int estimatedProgressPercent() {
        return (int) ((currentY * columns + currentX) / ((double) rows * columns) * 100);
    }
}
