package reconstruction.workers;

import data.image.AbstractBitmap;
import data.image.ImageResolution;
import reconstruction.MosaicFragment;
import util.image.ColorAnalysisUtil;
import util.image.ColorMetric;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by daniel on 08.06.17.
 */
public class MultiRectReconstructor extends RectReconstructor {

    private final List<ImageResolution> allowedResolutions;
    private final double similarityFactor;
    private final ColorMetric colorMetric;
    private final boolean useAlpha;
    private int currentRowIndex;
    private int currentColumnIndex;
    private boolean[][] rectIsUsed;
    private MosaicFragment wantedFragment;
    private int rectUsedCount;

    public MultiRectReconstructor(AbstractBitmap source, int wantedRows, int wantedColumns,
                                  List<ImageResolution> allowedResolutions, ColorMetric metric,
                                  boolean useAlpha, double similarityFactor) {
        super(source, wantedRows, wantedColumns);
        this.colorMetric = metric;
        this.useAlpha = useAlpha;
        this.allowedResolutions = allowedResolutions.stream().distinct().collect(Collectors.toList());
        if (this.allowedResolutions.size() == 0 || this.allowedResolutions.contains(null)) {
            throw new IllegalArgumentException("Needs at least one valid resolution.");
        }
        this.similarityFactor = Math.max(0., Math.min(1., similarityFactor));
        rectIsUsed = new boolean[getRows()][getColumns()];
        currentRowIndex = 0;
        currentColumnIndex = 0;
    }

    @Override
    public boolean giveNext(AbstractBitmap nextFragmentImage) {
        if (this.hasAll() || wantedFragment == null || nextFragmentImage == null
                || nextFragmentImage.getWidth() != wantedFragment.getWidth()
                || nextFragmentImage.getHeight() != wantedFragment.getHeight()) {
            return false;
        }
        int endRowIndex = currentRowIndex + wantedFragment.getHeight() / mRectHeight;
        int endColumnIndex = currentColumnIndex + wantedFragment.getWidth() / mRectWidth;
        markUsed(currentRowIndex, currentColumnIndex, endRowIndex, endColumnIndex);
        mResultCanvas.drawBitmap(nextFragmentImage, currentColumnIndex * mRectWidth,
                currentRowIndex * mRectHeight);
        advanceRectIndex();
        wantedFragment = null;
        return true;
    }

    private void markUsed(int startRow, int startColumn, int endRowIndex, int endColumnIndex) {
        for (int r = startRow; r < endRowIndex; r++) {
            for (int c = startColumn; c < endColumnIndex; c++) {
                if (rectIsUsed[r][c]) {
                    throw new IllegalStateException("Already used!" + r + "/" + c);
                }
                rectIsUsed[r][c] = true;
                rectUsedCount++;
            }
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
        Optional<MosaicFragment> max = allowedResolutions.stream()
                .map(resolution -> findGreatestWithResolution(currentRowIndex, currentColumnIndex, resolution))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .max(Comparator.comparingInt(frag -> frag.getHeight() * frag.getWidth()));
        wantedFragment = max.orElseGet(() -> new MosaicFragment(mRectWidth, mRectHeight, resultingRGBA[currentRowIndex][currentColumnIndex]));

        return wantedFragment;
    }

    private void advanceRectIndex() {
        while (currentRowIndex < getRows() && currentColumnIndex < getColumns()
                && rectIsUsed[currentRowIndex][currentColumnIndex]) {
            if (currentColumnIndex < getColumns()) {
                currentColumnIndex++;
            }
            if (currentColumnIndex == getColumns()) {
                currentColumnIndex = 0;
                currentRowIndex++;
            }
        }
    }

    private Optional<MosaicFragment> findGreatestWithResolution(int rowIndex, int columnIndex, ImageResolution resolution) {
        if (rectIsUsed[rowIndex][columnIndex]) {
            throw new IllegalStateException("Trying to reuse rect" + rowIndex + "/" + columnIndex);
        }
        int maxFittingResolutionMultiple = Math.min((getColumns() - columnIndex) / resolution.getWidth(),
                (getRows() - rowIndex) / resolution.getHeight());
        if (maxFittingResolutionMultiple <= 0) {
            return Optional.empty();
        }
        // we search for the first multiple that is not possible anymore
        OptionalInt first = IntStream.rangeClosed(1, maxFittingResolutionMultiple)
                .filter(i -> !checkRect(rowIndex, columnIndex, i, resolution))
                .findFirst();
        int biggestPossibleResolutionMultiple = maxFittingResolutionMultiple;
        if (first.isPresent()) {
            biggestPossibleResolutionMultiple = first.getAsInt() - 1;
        }
        int averageColor = ColorAnalysisUtil.getAverageColor((x, y) -> resultingRGBA[y][x], columnIndex, columnIndex + biggestPossibleResolutionMultiple * resolution.getWidth(),
                rowIndex, rowIndex + biggestPossibleResolutionMultiple * resolution.getHeight());
        return Optional.of(new MosaicFragment(mRectWidth * biggestPossibleResolutionMultiple * resolution.getWidth(),
                mRectHeight * biggestPossibleResolutionMultiple * resolution.getHeight(),
                averageColor));
    }

    private boolean checkRect(int rowIndex, int columnIndex, int resolutionMultiple, ImageResolution resolution) {
        final double simBound = ColorAnalysisUtil.factorToSimilarityBound(similarityFactor);
        final int baseColor = resultingRGBA[rowIndex][columnIndex];
        int endRow = rowIndex + resolutionMultiple * resolution.getHeight();
        int endColumn = columnIndex + resolutionMultiple * resolution.getWidth();
        int prevEndRow = endRow - resolution.getHeight();
        int prevEndColumn = endColumn - resolution.getWidth();
        // by assertion the rect for resolution multiple smaller than one is already checked
        // so we only need to check the three subrects needed to form the bigger rect
        boolean check1 = checkSubRect(baseColor, simBound, prevEndRow, columnIndex, endRow, endColumn);
        boolean check2 = checkSubRect(baseColor, simBound, rowIndex, prevEndColumn, prevEndRow, endColumn);
        return check1 && check2;
    }

    private boolean checkSubRect(int baseColor, double simBound, int startRow, int startColumn, int endRow, int endColumn) {

        final double maxSim = colorMetric.maxValue(useAlpha);
        for (int r = startRow; r < endRow; r++) {
            for (int c = startColumn; c < endColumn; c++) {
                if (rectIsUsed[r][c]) {
                    return false;
                }
                double currFactor = colorMetric.getDistance(baseColor, resultingRGBA[r][c],
                        useAlpha) / maxSim;
                if (currFactor > simBound) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public boolean hasAll() {
        return currentRowIndex >= getRows() || currentColumnIndex >= getColumns();
    }

    @Override
    public int estimatedProgressPercent() {
        return (int) (rectUsedCount / ((double) getRows() * getColumns()) * 100);
    }
}
