package matching;

import data.mosaic.MosaicTile;
import matching.workers.RandomMatcher;
import org.junit.Before;
import org.junit.Test;
import reconstruction.MosaicFragment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * Created by dd on 22.06.17.
 */
public class RandomMatcherTest {
    private List<MosaicTile<String>> tiles;
    private MosaicFragment fragment;

    @Before
    public void initTiles() {
        fragment = new MosaicFragment(200, 42, 0xFF00FF00);
        tiles = new ArrayList<>();
        tiles.add(new MockTile("S1", 0xFF000000, 50, 50));
        tiles.add(new MockTile("S2", 0xFFFF0000, 50, 50));
        tiles.add(new MockTile("S3", 0xFF000000, 100, 50));
    }

    @Test
    public void testEmptyMatch() {
        TileMatcher<String> matcher = new RandomMatcher<>(Collections.emptyList());
        assertEquals(0, matcher.getUsedTilesCount());
        assertEquals(0., matcher.getAccuracy(), 1E-10);
        assertFalse(matcher.setAccuracy(1.));
        assertEquals(0., matcher.getAccuracy(), 1E-10);
        assertFalse(matcher.usesAlpha());
        matcher.setUseAlpha(true);
        assertTrue(matcher.usesAlpha());
        matcher.setUseAlpha(false);
        assertTrue(matcher.cacheEnabled());
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragment());
        assertFalse(bestMatch.isPresent());
    }

    @Test
    public void testRemoveTile() {
        TileMatcher<String> matcher = new RandomMatcher<>(tiles);
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragment());
        assertTrue(bestMatch.isPresent());
        assertEquals(3, matcher.getUsedTilesCount());
        assertTrue(matcher.removeTile(bestMatch.get()));
        assertEquals(2, matcher.getUsedTilesCount());
        assertEquals(3, tiles.size());
    }

    @Test
    public void testRandomMatch() {
        TileMatcher<String> matcher = new RandomMatcher<>(tiles);
        assertEquals(3, matcher.getUsedTilesCount());
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragment());
        assertTrue(bestMatch.isPresent());
        assertTrue(tiles.contains(bestMatch.get()));
    }

    public MosaicFragment getFragment() {
        return fragment;
    }
}
