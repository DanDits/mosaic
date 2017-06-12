package reconstruction.workers;

import data.image.*;
import reconstruction.MosaicFragment;
import reconstruction.ReconstructionParameters;
import reconstruction.Reconstructor;
import util.image.ColorAnalysisUtil;

import java.util.Random;

/**
 * Created by dd on 07.06.17.
 */
public class PuzzleReconstructor extends Reconstructor {
    private static final double NOSE_SPACE_FRACTION = 0.29;
    private static final int[] BORDER_COLORS = {0xFF1E1E1E, 0xFF7F7F7F, 0xFFAAAAAA, 0xFFD4D4D4};
    private static final int BORDER_THICKNESS = BORDER_COLORS.length;
    private final int rectHeight;
    private final int rectWidth;
    private final AbstractCanvas resultCanvas;
    private final int columns;
    private final int rows;
    private AbstractBitmap borderRight;
    private AbstractBitmap borderDown;
    private AbstractBitmap source;
    private AbstractBitmap result;
    private Random random;
    private int currentX;
    private int currentY;
    private PuzzlePiece[] prevRow;
    private PuzzlePiece[] currentRow;
    private MosaicFragment wantedFragment;

    public static class PuzzleParameters extends ReconstructionParameters {
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
        this.rectHeight = source.getHeight() / actualRows;
        this.rectWidth = source.getWidth() / actualColumns;
        random = new Random();
        this.result = obtainBaseBitmap(this.rectWidth * actualColumns, this.rectHeight * actualRows);
        resultCanvas = AbstractCanvasFactory.getInstance().makeCanvas(result);
        createBorders();
    }

    private void createBorders() {
        int borderWidth = Math.min(rectWidth, BORDER_THICKNESS);
        int borderHeight = (int) (rectHeight * (1 - NOSE_SPACE_FRACTION) / 2.);
        // TODO we can make a custom stroke for this, see http://www.java2s.com/Code/Java/2D-Graphics-GUI/CustomStrokes.htm
        // TODO and also use this to create the puzzle nose
        // TODO but this does not allow different colors?! and breaks portability kinda
        AbstractCanvas rightCanvas = AbstractCanvasFactory.getInstance().makeCanvas(borderWidth, borderHeight);
        rightCanvas.drawColor(0xFFFFFFFF);
        for (int i = 0; i < Math.min(borderWidth, borderHeight); i++) {
            rightCanvas.drawLine(borderWidth - 1 - i, i,
                    borderWidth - 1 - i, borderHeight - 1 - i,
                    BORDER_COLORS[i]);
        }
        borderRight = rightCanvas.obtainImage();

        borderWidth = (int) (rectWidth * (1 - NOSE_SPACE_FRACTION) / 2.);
        borderHeight = Math.min(rectHeight, BORDER_THICKNESS);
        AbstractCanvas downCanvas = AbstractCanvasFactory.getInstance().makeCanvas(borderWidth, borderHeight);
        downCanvas.drawColor(0xFFFFFFFF);
        for (int i = 0; i < Math.min(borderWidth, borderHeight); i++) {
            downCanvas.drawLine(i, borderHeight - 1 - i,
                    borderWidth - 1 - i, borderHeight - 1 - i,
                    BORDER_COLORS[i]);
        }
        borderDown = downCanvas.obtainImage();
    }

    private void createNoses() {
        int noseWidth = getPuzzleHorizontalNoseLength();
        int noseHeight = (int) (NOSE_SPACE_FRACTION * rectHeight);
        AbstractCanvas canvas = AbstractCanvasFactory.getInstance().makeCanvas(noseWidth, noseHeight);
        canvas.drawColor(AbstractColor.TRANSPARENT);
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
            return rectWidth + (genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0)
                    + (genderRight == 1 ? getPuzzleHorizontalNoseLength() : 0);
        }

        private int getRequiredHeight() {
            return rectHeight + (genderUp == 1 ? getPuzzleVerticalNoseLength() : 0)
                    + (genderDown == 1 ? getPuzzleVerticalNoseLength() : 0);
        }

        public int getAverageColor() {
            int startX = posX * rectWidth - (genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0);
            int startY = posY * rectHeight - (genderUp == 1 ? getPuzzleVerticalNoseLength() : 0);
            int endX = (posX + 1) * rectWidth + (genderRight == 1 ? getPuzzleHorizontalNoseLength() : 0);
            int endY = (posY + 1) * rectHeight + (genderDown == 1 ? getPuzzleVerticalNoseLength() : 0);
            return ColorAnalysisUtil.getAverageColor(source, startX, endX, startY, endY);
        }

        public void draw(AbstractBitmap bitmap) {
            this.bitmap = bitmap;
            drawInner();
            drawBorders();
            drawUpper();
            drawLeft();
        }

        private void drawBorders() {
            resultCanvas.drawMultiplicativly(borderRight, (posX + 1) * rectWidth - borderRight.getWidth(),
                    posY * rectHeight);
            resultCanvas.drawMultiplicativly(borderRight, (posX + 1) * rectWidth - borderRight.getWidth(),
                    (posY + 1) * rectHeight - borderRight.getHeight());
            resultCanvas.drawMultiplicativly(borderDown, posX * rectWidth,
                    (posY + 1) * rectHeight - borderDown.getHeight());
            resultCanvas.drawMultiplicativly(borderDown, (posX + 1) * rectWidth - borderDown.getWidth(),
                    (posY + 1) * rectHeight - borderDown.getHeight());
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
            resultCanvas.drawBitmap(bitmap, posX * rectWidth, posY * rectHeight, startX, startY, endX, endY);
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
        return random.nextBoolean() ? 1 : -1; // no offense
    }

    private int getPuzzleHorizontalNoseLength() {
        return (int) (rectWidth * 0.2);
    }

    private int getPuzzleVerticalNoseLength() {
        return (int) (rectHeight * 0.2);
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
