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
    private static final double NOSE_SPACE_FRACTION = 0.2;
    private static final double NOSE_THICKNESS_FRACTION = 0.4;
    private static final int[] BORDER_COLORS = {0xFF7F7F7F, 0xFFAAAAAA, 0xFFD4D4D4, 0xFFEEEEEE};
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
    private AbstractBitmap noseHorizontalRight;
    private AbstractBitmap noseHorizontalLeft;
    private AbstractBitmap noseVerticalUp;
    private AbstractBitmap noseVerticalDown;
    private AbstractCanvas noseBufferHorizontal;
    private AbstractCanvas noseBufferVertical;


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
        createNoses();
    }

    private void createBorders() {
        int borderWidth = Math.min(rectWidth, BORDER_THICKNESS);
        int borderHeight = (int) (rectHeight * (1 - NOSE_SPACE_FRACTION) / 2.);

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
        noseHorizontalRight = createNose(getPuzzleHorizontalNoseLength(), (int) (NOSE_THICKNESS_FRACTION * rectHeight), rectHeight, true);
        noseHorizontalLeft = noseHorizontalRight.getRotatedCopy(180);
        noseVerticalDown = createNose((int) (NOSE_THICKNESS_FRACTION * rectWidth), getPuzzleVerticalNoseLength(), rectWidth, false);
        noseVerticalUp = noseVerticalDown.getRotatedCopy(180);
        AbstractBitmap buffer1 = AbstractBitmapFactory.makeInstance(noseHorizontalRight.getWidth(), noseHorizontalRight.getHeight()).createBitmap();
        noseBufferHorizontal = AbstractCanvasFactory.getInstance().makeCanvas(buffer1);
        AbstractBitmap buffer2 = AbstractBitmapFactory.makeInstance(noseVerticalDown.getWidth(), noseVerticalDown.getHeight()).createBitmap();
        noseBufferVertical = AbstractCanvasFactory.getInstance().makeCanvas(buffer2);

    }

    private static AbstractBitmap createNose(int noseWidth, int noseHeight, int noseStartAreaDimension, boolean horizontal) {
        if (!horizontal) {
            int temp = noseWidth;
            noseWidth = noseHeight;
            noseHeight = temp;
        }
        // if we render the quadratic curves with antialiasing enabled we get visual problems when drawing the nose
        // multiplicativly onto the puzzle piece bitmap as bright pixels are inside or outside the border line
        AbstractCanvas canvas = AbstractCanvasFactory.getInstance().makeCanvas(noseWidth, noseHeight);
        int innerColor = 0xFFFFFFFF;
        canvas.drawColor(innerColor);
        int color = BORDER_COLORS[0];
        int startOffsetY = (int) (noseStartAreaDimension / 2. * (NOSE_THICKNESS_FRACTION - NOSE_SPACE_FRACTION));

        int firstPartMiddleX = (int) (noseWidth * 0.3);
        int firstPartMiddleY = (int) (startOffsetY * 1.);
        int firstPartEndX = (int) (noseWidth * 0.4);
        int firstPartEndY = (int) (startOffsetY * 0.6);
        canvas.drawQuadraticCurve(0, startOffsetY, firstPartMiddleX, firstPartMiddleY, firstPartEndX, firstPartEndY, color);

        int secondPartMiddleX = (int) (noseWidth * 0.45);
        int secondPartMiddleY = (int) (0.00 * startOffsetY);
        int secondPartEndX = (int) ( noseWidth * 0.7);
        int secondPartEndY = 0;
        canvas.drawQuadraticCurve(firstPartEndX, firstPartEndY, secondPartMiddleX, secondPartMiddleY, secondPartEndX, secondPartEndY, color);

        int thirdPartMiddleX = (int) (noseWidth * 0.95);
        int thirdPartMiddleY = (int) (0.15 * startOffsetY);
        int thirdPartEndX = (int) (noseWidth * 1.);
        int thirdPartEndY = noseHeight / 2;
        canvas.drawQuadraticCurve(secondPartEndX, secondPartEndY, thirdPartMiddleX, thirdPartMiddleY, thirdPartEndX, thirdPartEndY,color);

        canvas.drawQuadraticCurve(0, noseHeight - startOffsetY - 1, firstPartMiddleX, noseHeight - firstPartMiddleY, firstPartEndX, noseHeight - firstPartEndY - 1, color);
        canvas.drawQuadraticCurve(firstPartEndX, noseHeight - firstPartEndY - 1, secondPartMiddleX, noseHeight - secondPartMiddleY, secondPartEndX, noseHeight - secondPartEndY - 1, color);
        canvas.drawQuadraticCurve(secondPartEndX, noseHeight - secondPartEndY - 1, thirdPartMiddleX, noseHeight - thirdPartMiddleY, thirdPartEndX, noseHeight - thirdPartEndY - 1, color);

        canvas.floodFill(0, 0, AbstractColor.TRANSPARENT);
        canvas.floodFill(0, noseHeight - 1, AbstractColor.TRANSPARENT);
        canvas.floodFill(noseWidth - 1, 0, AbstractColor.TRANSPARENT);
        canvas.floodFill(noseWidth - 1, noseHeight - 1, AbstractColor.TRANSPARENT);
        AbstractBitmap nose = canvas.obtainImage();
        if (!horizontal) {
            return nose.getRotatedCopy(90);
        }
        return canvas.obtainImage();

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
            if (genderLeft == 1) {
                int fromY = (int) ((1 - NOSE_THICKNESS_FRACTION) * rectHeight / 2);
                fromY += genderUp == 1 ? getPuzzleVerticalNoseLength() : 0;
                drawNose(noseHorizontalLeft, noseBufferHorizontal, posX * rectWidth - noseHorizontalLeft.getWidth(),
                        (int) (posY * rectHeight + (1 - NOSE_THICKNESS_FRACTION) * rectHeight / 2),
                        0, fromY);
            }
        }

        private void drawRight() {
            if (genderRight == 1) {
                int fromX = rectWidth;
                fromX += genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0;
                int fromY = (int) ((1 - NOSE_THICKNESS_FRACTION) * rectHeight / 2);
                fromY += genderUp == 1 ? getPuzzleVerticalNoseLength() : 0;
                drawNose(noseHorizontalRight, noseBufferHorizontal,
                        (posX + 1) * rectWidth,
                        (int) (posY * rectHeight + (1 - NOSE_THICKNESS_FRACTION) * rectHeight / 2),
                        fromX, fromY);
            }
        }

        public void drawInner() {
            int startX = genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0;
            int startY = genderUp == 1 ? getPuzzleVerticalNoseLength() : 0;
            int endX = genderRight == 1 ? bitmap.getWidth() - getPuzzleHorizontalNoseLength() : bitmap.getWidth();
            int endY = genderDown == 1 ? bitmap.getHeight() - getPuzzleVerticalNoseLength() : bitmap.getHeight();
            resultCanvas.drawBitmap(bitmap, posX * rectWidth, posY * rectHeight, startX, startY, endX, endY);
        }

        public void drawNose(AbstractBitmap nose, AbstractCanvas buffer, int x, int y, int fromX, int fromY) {
            //buffer.drawBitmapUsingPorterDuff(buffer.obtainImage(), 0, 0, PorterDuffMode.CLEAR);
            buffer.drawBitmap(nose, 0, 0);
            buffer.drawMultiplicativly(bitmap, 0, 0, fromX, fromY, fromX + nose.getWidth(), fromY + nose.getHeight());
            buffer.drawBitmapUsingPorterDuff(nose, 0, 0, PorterDuffMode.DESTINATION_IN);
            resultCanvas.drawBitmapUsingPorterDuff(buffer.obtainImage(), x, y, PorterDuffMode.SOURCE_OVER);
        }

        public void drawUpper() {
            if (genderUp == -1) {
                upperPiece.drawLower();
            }
            if (genderUp == 1) {
                int fromX = (int) ((1 - NOSE_THICKNESS_FRACTION) * rectWidth / 2);
                fromX += genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0;
                drawNose(noseVerticalUp, noseBufferVertical,
                        (int) (posX * rectWidth + (1 - NOSE_THICKNESS_FRACTION) * rectWidth / 2.),
                        posY * rectHeight - noseVerticalUp.getHeight(),
                        fromX, 0);
            }
        }

        public void drawLower() {
            if (genderDown == 1) {
                int fromX = (int) ((1 - NOSE_THICKNESS_FRACTION) * rectWidth / 2);
                fromX += genderLeft == 1 ? getPuzzleHorizontalNoseLength() : 0;
                int fromY = rectHeight;
                fromY += genderUp == 1 ? getPuzzleVerticalNoseLength() : 0;
                drawNose(noseVerticalDown, noseBufferVertical,
                        (int) (posX * rectWidth + (1 - NOSE_THICKNESS_FRACTION) * rectWidth / 2.),
                        (posY + 1) * rectHeight,
                        fromX, fromY);
            }
        }

    }

    private int getNewGender() {
        return random.nextBoolean() ? 1 : -1; // no offense
    }

    private int getPuzzleHorizontalNoseLength() {
        return (int) (rectWidth * 0.33);
    }

    private int getPuzzleVerticalNoseLength() {
        return (int) (rectHeight * 0.33);
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
