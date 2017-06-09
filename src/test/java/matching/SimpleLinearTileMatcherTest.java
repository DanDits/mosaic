package matching;

import data.AbstractColor;
import data.MosaicTile;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import reconstruction.MosaicFragment;
import util.image.ColorMetric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by dd on 07.06.17.
 */
public class SimpleLinearTileMatcherTest {

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
        ColorMetric metric = ColorMetric.Euclid2.INSTANCE;
        TileMatcher<String> matcher = new SimpleLinearTileMatcher<>(Collections.emptyList(), true, metric);
        assertEquals(0, matcher.getUsedTilesCount());
        assertEquals(1., matcher.getAccuracy(), 1E-10);
        matcher.setAccuracy(0.3);
        assertEquals(1., matcher.getAccuracy(), 1E-10);
        assertTrue(matcher.usesAlpha());
        matcher.setUseAlpha(false);
        assertFalse(matcher.usesAlpha());
        matcher.setUseAlpha(true);
        assertFalse(matcher.getBestMatch(getFragmentForColor(0xFFFF0000)).isPresent());
    }

    @Test
    public void testExactMatch() {
        ColorMetric metric = ColorMetric.Euclid2.INSTANCE;
        TileMatcher<String> matcher = new SimpleLinearTileMatcher<>(tiles, true, metric);
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForColor(0xFFFF0000));
        assertTrue(bestMatch.isPresent());
        assertEquals("S1", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForColor(0xAAFF0000));
        assertTrue(bestMatch.isPresent());
        assertEquals("S2", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForColor(0xFFFFFF00));
        assertTrue(bestMatch.isPresent());
        assertEquals("S3", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForColor(0xFFFF00FF));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());
    }

    @Test
    public void testInexactMatch() {
        ColorMetric metric = ColorMetric.Euclid2.INSTANCE;
        TileMatcher<String> matcher = new SimpleLinearTileMatcher<>(tiles, true, metric);
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForColor(0xFFFA0105));
        assertTrue(bestMatch.isPresent());
        assertEquals("S1", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForColor(0xAAFF00FE));
        assertTrue(bestMatch.isPresent());
        assertEquals("S5", bestMatch.get().getSource());

        // this also tests if the hashed entry to the previously given color has been properly reset
        matcher.setUseAlpha(false);
        assertFalse(matcher.usesAlpha());
        bestMatch = matcher.getBestMatch(getFragmentForColor(0xAAFF00FE));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());
    }
}
