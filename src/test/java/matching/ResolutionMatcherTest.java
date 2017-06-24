package matching;

import data.mosaic.MosaicTile;
import matching.workers.ResolutionMatcher;
import org.junit.Before;
import org.junit.Test;
import reconstruction.MosaicFragment;
import util.image.ColorSpace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;


public class ResolutionMatcherTest {

    private List<MosaicTile<String>> tiles;
    private static final int DEFAULT_WANTED_COLOR = 0xFF000000; // black

    @Before
    public void initTiles() {
        tiles = new ArrayList<>();
        tiles.add(new MockTile("S0", 0xFFFF0000, 4, 4)); // 1:1
        tiles.add(new MockTile("S1", 0xFFFF0000, 4, 8)); // 1:2
        tiles.add(new MockTile("S3", 0xAAFF0000, 100, 149)); // almost 2:3
        tiles.add(new MockTile("S4", DEFAULT_WANTED_COLOR, 100, 151)); // almost 2:3
        tiles.add(new MockTile("S2", 0xAAFF0000, 4, 6)); // 2:3
        tiles.add(new MockTile("S5", 0xFFFFFF00, 3, 2)); // 3:2
    }

    private MosaicFragment getFragmentForResolution(int width, int height) {
        return new MosaicFragment(width, height, DEFAULT_WANTED_COLOR);
    }


    @Test
    public void testRemoveTile() {
        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        TileMatcher<String> matcher = new ResolutionMatcher<>(tiles, 1., space);
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForResolution(100, 148));
        assertTrue(bestMatch.isPresent());

        assertEquals(6, matcher.getUsedTilesCount());
        assertTrue(matcher.removeTile(bestMatch.get()));
        assertEquals(5, matcher.getUsedTilesCount());
        assertEquals(6, tiles.size());
    }

    @Test
    public void testEmptyMatch() {
        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        double acc = 0.9;
        TileMatcher<String> matcher = new ResolutionMatcher<>(Collections.emptyList(), acc, space);
        assertEquals(0, matcher.getUsedTilesCount());
        assertEquals(acc, matcher.getAccuracy(), 1E-10);
        acc = 0.3;
        matcher.setAccuracy(acc);
        assertEquals(acc, matcher.getAccuracy(), 1E-10);
        assertTrue(matcher.usesAlpha());
        matcher.setUseAlpha(false);
        assertFalse(matcher.usesAlpha());
        matcher.setUseAlpha(true);
        assertFalse(matcher.getBestMatch(getFragmentForResolution(2, 3)).isPresent());
        assertTrue(matcher.cacheEnabled());
    }

    @Test
    public void testInexactMatchFullAccuracy() {
        double acc = 1.; // will drop every tile and search for the one with the closest resolution difference

        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        TileMatcher<String> matcher = new ResolutionMatcher<>(tiles, acc, space);

        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForResolution(100, 148));
        assertTrue(bestMatch.isPresent());
        assertEquals("S3", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(100, 152));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(1000, 1501));
        assertTrue(bestMatch.isPresent());
        assertEquals("S2", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(10, 9));
        assertTrue(bestMatch.isPresent());
        assertEquals("S0", bestMatch.get().getSource());
    }

    @Test
    public void testInexactMatchMediumAccuracy() {
        double acc = 0.5;

        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        TileMatcher<String> matcher = new ResolutionMatcher<>(tiles, acc, space);

        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForResolution(100, 148));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(10, 9));
        assertTrue(bestMatch.isPresent());
        assertEquals("S0", bestMatch.get().getSource());
    }

    @Test
    public void testExactMatchFullAccuracy() {
        double acc = 1.;

        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        TileMatcher<String> matcher = new ResolutionMatcher<>(tiles, acc, space);
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForResolution(2, 3));
        assertTrue(bestMatch.isPresent());
        assertEquals("S2", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(3, 2));
        assertTrue(bestMatch.isPresent());
        assertEquals("S5", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(1, 2));
        assertTrue(bestMatch.isPresent());
        assertEquals("S1", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(1, 1));
        assertTrue(bestMatch.isPresent());
        assertEquals("S0", bestMatch.get().getSource());
    }

    @Test
    public void testExactMatchMediumAccuracy() {
        double acc = 0.5;

        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        TileMatcher<String> matcher = new ResolutionMatcher<>(tiles, acc, space);
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForResolution(2, 3));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(3, 2));
        assertTrue(bestMatch.isPresent());
        assertEquals("S5", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(1, 2));
        assertTrue(bestMatch.isPresent());
        assertEquals("S1", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(1, 1));
        assertTrue(bestMatch.isPresent());
        assertEquals("S0", bestMatch.get().getSource());
    }

    @Test
    public void testExactMatchNoAccuracy() {
        double acc = 0.0; // basically a SimpleLinearTileMatcher now

        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;
        TileMatcher<String> matcher = new ResolutionMatcher<>(tiles, acc, space);
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragmentForResolution(2, 3));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(3, 2));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(1, 2));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());

        bestMatch = matcher.getBestMatch(getFragmentForResolution(1, 1));
        assertTrue(bestMatch.isPresent());
        assertEquals("S4", bestMatch.get().getSource());
    }

}
