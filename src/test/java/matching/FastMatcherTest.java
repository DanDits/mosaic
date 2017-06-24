package matching;

import data.mosaic.MosaicTile;
import matching.workers.FastMatcher;
import org.junit.Before;
import org.junit.Test;
import reconstruction.MosaicFragment;
import util.image.ColorSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class FastMatcherTest {
    private List<MosaicTile<String>> tiles;

    @Before
    public void initTiles() {
        tiles = new ArrayList<>();
        tiles.add(new MockTile("S1", 0xFFFF0000, 0, 0));
        tiles.add(new MockTile("S2", 0xAAFF0000, 0, 0));
        tiles.add(new MockTile("S3", 0xFFFFFF00, 0, 0));
        tiles.add(new MockTile("S4", 0xFFFF00FE, 0, 0));
        tiles.add(new MockTile("S5", 0xAAFF00FF, 0, 0));
    }

    private MosaicFragment getFragmentForColor(int color) {
        return new MosaicFragment(0, 0, color);
    }

    @Test
    public void testEmptyMatch() {
        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        TileMatcher<String> matcher = new FastMatcher<>(Collections.emptyList(), space);
        assertEquals(0, matcher.getUsedTilesCount());
        assertEquals(1., matcher.getAccuracy(), 1E-10);
        matcher.setAccuracy(0.3);
        assertEquals(1., matcher.getAccuracy(), 1E-10);
        assertTrue(matcher.usesAlpha());
        matcher.setUseAlpha(false);
        assertFalse(matcher.usesAlpha());
        matcher.setUseAlpha(true);
        assertFalse(matcher.getBestMatch(getFragmentForColor(0xFFFF0000)).isPresent());
        assertTrue(matcher.cacheEnabled());
    }

    //TODO implement other tests
}
