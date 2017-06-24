package matching;

import data.mosaic.MosaicTile;
import matching.workers.TrivialMatcher;
import org.junit.Test;
import reconstruction.MosaicFragment;

import java.util.Optional;
import java.util.Random;

import static org.junit.Assert.*;

public class TrivialMatcherTest {

    private MosaicFragment getFragment(int seed) {
        Random rnd = new Random(seed);
        return new MosaicFragment(rnd.nextInt(Integer.MAX_VALUE),
                rnd.nextInt(Integer.MAX_VALUE), rnd.nextInt());
    }

    @Test
    public void testEmptyMatch() {
        TileMatcher<String> matcher = new TrivialMatcher<>();
        assertEquals(1, matcher.getUsedTilesCount());
        assertEquals(1., matcher.getAccuracy(), 1E-10);
        assertFalse(matcher.setAccuracy(0.));
        assertEquals(1., matcher.getAccuracy(), 1E-10);
        assertFalse(matcher.usesAlpha());
        matcher.setUseAlpha(true);
        assertTrue(matcher.usesAlpha());
        matcher.setUseAlpha(false);
        assertFalse(matcher.cacheEnabled());
        int seed = 1337;
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragment(seed));
        assertTrue(bestMatch.isPresent());
        assertTrue(tileEqualsFragment(bestMatch.get(), getFragment(seed)));
    }

    @Test
    public void testRemoveTile() {
        TileMatcher<String> matcher = new TrivialMatcher<>();
        int seed = 1337;
        Optional<? extends MosaicTile<String>> bestMatch = matcher.getBestMatch(getFragment(seed));
        assertTrue(bestMatch.isPresent());
        assertEquals(1, matcher.getUsedTilesCount());
        assertFalse(matcher.removeTile(bestMatch.get()));
        assertEquals(1, matcher.getUsedTilesCount());
    }

    private boolean tileEqualsFragment(MosaicTile<String> tile, MosaicFragment fragment) {
        return tile.getAverageARGB() == fragment.getAverageRGB() && tile.getWidth() == fragment.getWidth() &&
                tile.getHeight() == fragment.getHeight();
    }

}
