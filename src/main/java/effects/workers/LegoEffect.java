package effects.workers;

import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.image.AbstractCanvas;
import data.image.AbstractCanvasFactory;
import data.storage.MosaicTile;
import data.storage.VoidTile;
import matching.TileMatcher;
import matching.workers.SimpleLinearTileMatcher;
import reconstruction.MosaicFragment;
import util.image.ColorSpace;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspired by https://github.com/JuanPotato/Legofy
 * Created by dd on 28.06.17.
 */
public class LegoEffect extends RectPatternEffect {
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

    private static final String LEGO_PATH = "res/images/lego_blueprint.png";
    private final AbstractBitmap legoBitmap;
    private final boolean usePalettes;
    private TileMatcher<Void> matcher;
    private AbstractCanvas rectCanvas;
    private AbstractBitmap rectBitmap;

    public LegoEffect(ColorSpace space, int wantedRows, int wantedColumns, int groundingColor, boolean usePalettes) {
        super(space, wantedRows, wantedColumns, groundingColor);
        legoBitmap = AbstractBitmapFactory.makeInstance(new File(LEGO_PATH)).createBitmap();
        this.usePalettes = usePalettes;
    }

    @Override
    protected void drawRect(AbstractCanvas canvas, int fromX, int fromY, int toX, int toY, int analyzedArgb) {
        int width = toX - fromX;
        int height = toY - fromY;
        ensureLegoBitmapSize(width, height);
        int colorToDraw = getColorToDraw(analyzedArgb);
        ensureRectCanvas(width, height);
        rectCanvas.drawColor(colorToDraw);
        rectCanvas.drawMultiplicativly(legoBitmap, 0, 0);
        canvas.drawBitmap(rectBitmap, fromX, fromY);
    }

    private void ensureRectCanvas(int width, int height) {
        if (rectCanvas == null) {
            rectBitmap = AbstractBitmapFactory.makeInstance(width, height).createBitmap();
            rectCanvas = AbstractCanvasFactory.getInstance().makeCanvas(rectBitmap);
        }
    }

    private int getColorToDraw(int wantedColor) {
        if (usePalettes) {
            if (matcher == null) {
                List<MosaicTile<Void>> tiles = new ArrayList<>();
                for (int value : LEGO_COLOR_PALETTE_SOLID) {
                    tiles.add(new VoidTile(value));
                }
                for (int value : LEGO_COLOR_PALETTE_TRANSPARENT) {
                    tiles.add(new VoidTile(value));
                }
                for (int value : LEGO_COLOR_PALETTE_EFFECTS) {
                    tiles.add(new VoidTile(value));
                }
                matcher = new SimpleLinearTileMatcher<>(tiles, space);
            }
            return matcher.getBestMatch(new MosaicFragment(0, 0, wantedColor)).map(MosaicTile::getAverageARGB)
                   .orElse(wantedColor);
        }
        return wantedColor;
    }

    private void ensureLegoBitmapSize(int width, int height) {
        if (legoBitmap.getWidth() != width || legoBitmap.getHeight() != height) {
            legoBitmap.resize(width, height);
        }
    }
}
