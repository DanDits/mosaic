package reconstruction.workers;

import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import data.storage.MosaicTile;
import matching.TileMatcher;
import matching.workers.SimpleLinearTileMatcher;
import matching.workers.TrivialMatcher;
import org.pmw.tinylog.Logger;
import reconstruction.ReconstructionParameters;
import reconstruction.pattern.PatternReconstructor;
import reconstruction.pattern.PatternSource;
import util.image.ColorAnalysisUtil;
import util.image.ColorSpace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspired by https://github.com/JuanPotato/Legofy
 * Created by daniel on 05.12.15.
 */
public class LegoPatternReconstructor extends PatternReconstructor {
    // lego colors see: http://www.brickjournal.com/files/PDFs/2010LEGOcolorpalette.pdf
    private static final int[] LEGO_COLOR_PALETTE_SOLID = new int[] {0xff311007, 0xff2d1678,
            0xff95b90c, 0xff8d7553, 0xfff4f4f4, 0xff017c29, 0xffa83e16, 0xfffec401, 0xff4d5e57,
            0xff9c01c6, 0xfff5c189, 0xff013517, 0xff020202, 0xffaa7e56, 0xff0158a8, 0xff5f758c,
            0xffde010e, 0xffee9dc3, 0xff87c0ea, 0xfff49b01, 0xffffff99, 0xffd67341, 0xff608266,
            0xff9c9291, 0xff80091c, 0xff019625, 0xff488cc6, 0xffd9bb7c, 0xff5c1d0d, 0xffde388b,
            0xffe76419, 0xffe4e4da, 0xff012642};
    private static final int[] LEGO_COLOR_PALETTE_TRANSPARENT = new int[] {0xaaf9ef69, 0xaaeeeeee,
            0xaae76648, 0xaa50b1e8, 0xaaec760e, 0xaaa69182, 0xaab6e0ea, 0xaa9c95c7, 0xaa99ff66,
            0xaaee9dc3, 0xaa63b26e, 0xaae02a29, 0xaaf1ed5b, 0xaacee3f6};
    private static final int[] LEGO_COLOR_PALETTE_EFFECTS = new int[] {0xff8d9496, 0xffaa7f2e,
            0xfffefcd5, 0xff493f3b};


    public static final String NAME = "Lego";
    private static final String LEGO_PATH = "res/images/lego_blueprint.png";
    private final AbstractBitmap mLegoBitmap;
    private final boolean usePalettes;

    public static class LegoParameters extends PatternParameters {
        public boolean usePalettes;

        @Override
        public void resetToDefaults() {
            super.resetToDefaults();
            usePalettes = true;
        }

        @Override
        public PatternReconstructor makeReconstructor() throws IllegalParameterException {
            return new LegoPatternReconstructor(this);
        }

        @Override
        public ColorSpace getColorSpace(ColorSpace defaultSpace) {
            return defaultSpace;
        }
    }

    public LegoPatternReconstructor(LegoParameters parameters) throws ReconstructionParameters.IllegalParameterException {
        super(parameters);
        usePalettes = parameters.usePalettes;
        mLegoBitmap = AbstractBitmapFactory.makeInstance(new File(LEGO_PATH)).createBitmap();
        if (mLegoBitmap == null) {
            Logger.error("Could not load lego bitmap from " + LEGO_PATH);
        }
    }

    public static class Source<S> extends PatternSource<S> {
        private final AbstractBitmap legoBitmap;
        private AbstractBitmap mPatternBitmap;
        private AbstractCanvas mCanvas;

        public Source(AbstractBitmap legoBitmap) {
            this.legoBitmap = legoBitmap;
        }

        @Override
        protected AbstractBitmap makePattern(int color, AbstractBitmap base) {
            if (legoBitmap.getWidth() != base.getWidth() || legoBitmap.getHeight() != base.getHeight()) {
                legoBitmap.resize(base.getWidth(), base.getHeight());
            }
            AbstractCanvas canvas = mCanvas;
            canvas.drawColor(color);
            canvas.drawMultiplicativly(legoBitmap, 0, 0);
            return base;
        }

        protected AbstractBitmap obtainBitmap(int key, int width, int height) {
            if (mPatternBitmap != null && mPatternBitmap.getWidth() == width && mPatternBitmap
                    .getHeight() == height) {
                return mPatternBitmap;
            }
            mPatternBitmap = super.obtainBitmap(key, width, height);
            mCanvas = AbstractCanvasFactory.getInstance().makeCanvas(mPatternBitmap);
            return mPatternBitmap;
        }

    }

    @Override
    protected int evaluateRectValue(AbstractBitmap source, int startX, int endX, int startY, int endY) {
        return ColorAnalysisUtil.getAverageColor(source, startX, endX, startY, endY);
    }

    @Override
    public <S> PatternSource<S> makeSource() {
        return new Source<>(mLegoBitmap);
    }

    @Override
    public <S> TileMatcher<S> makeMatcher(ColorSpace space) {
        if (usePalettes) {
            List<MosaicTile<S>> tiles = new ArrayList<>();
            for (int value : LEGO_COLOR_PALETTE_SOLID) {
                tiles.add(new VoidTile<>(value));
            }
            for (int value : LEGO_COLOR_PALETTE_TRANSPARENT) {
                tiles.add(new VoidTile<>(value));
            }
            for (int value : LEGO_COLOR_PALETTE_EFFECTS) {
                tiles.add(new VoidTile<>(value));
            }
            return new SimpleLinearTileMatcher<>(tiles, space);
        } else {
            return new TrivialMatcher<>();
        }
    }

    private static class VoidTile<S> implements MosaicTile<S> {

        private final int mLegoColor;

        public VoidTile(int color) {
            mLegoColor = color;
        }
        @Override
        public S getSource() {
            return null;
        }

        @Override
        public int getAverageARGB() {
            return mLegoColor;
        }

        @Override
        public int getWidth() {
            return 0;
        }

        @Override
        public int getHeight() {
            return 0;
        }
    }

}
