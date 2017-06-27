package matching;

import data.mosaic.MosaicTile;
import matching.workers.FastMatcher;
import matching.workers.RandomMatcher;
import matching.workers.ResolutionMatcher;
import matching.workers.SimpleLinearTileMatcher;
import org.junit.Before;
import org.junit.Test;
import reconstruction.MosaicFragment;
import util.image.ColorSpace;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by dd on 24.06.17.
 */
public class TileMatcherTest {
    private ArrayList<TileMatcher<String>> matchers;
    private ArrayList<MosaicTile<String>> tiles;

    @Before
    public void initMatchers() {
        tiles = new ArrayList<>();
        tiles.add(new MockTile("S1", 0xFFFF0000, 1, 1));
        tiles.add(new MockTile("S2", 0xAAFF0000, 2, 3));
        tiles.add(new MockTile("S3", 0xFFFFFF00, 2, 3));
        tiles.add(new MockTile("S4", 0xFFFF00FE, 3, 2));
        tiles.add(new MockTile("S5", 0xAAFF00FF, 3, 2));

        ColorSpace space = ColorSpace.RgbEuclid.INSTANCE_WITH_ALPHA;

        matchers = new ArrayList<>();
        matchers.add(new FastMatcher<>(tiles, space));
        matchers.add(new RandomMatcher<>(tiles));
        matchers.add(new ResolutionMatcher<>(tiles, 1., space));
        matchers.add(new SimpleLinearTileMatcher<>(tiles, space));
    }

    private List<String> getSources(int width, int height, int argb) {
        return matchers.stream().map(matcher -> matcher.getBestMatch(new MosaicFragment(width, height, argb)))
                       .map(Optional::get).map(MosaicTile::getSource).collect(Collectors.toList());
    }

    @Test
    public void testNoReuse() {
        matchers.forEach(matcher -> matcher.setTileReuseLimit(TileMatcher.REUSE_NONE));
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 5));
        List<String> first = getSources(1, 1, 0xFFFF0000);
        List<String> second = getSources(1, 1, 0xFFFF0000);
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 4));
        for (int i = 0; i < first.size(); i++) {
            assertFalse(first.get(i).equals(second.get(i)));
        }

    }

    @Test
    public void testSingleReuse() {
        matchers.forEach(matcher -> matcher.setTileReuseLimit(1));
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 5));
        List<String> first = getSources(1, 1, 0xFFFF0000);
        List<String> second = getSources(1, 1, 0xFFFF0000);
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 5));
        for (int i = 0; i < first.size(); i++) {
            assertTrue(first.get(i).equals(second.get(i)));
        }
        List<String> third = getSources(1, 1, 0xFFFF0000);
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 4));
        for (int i = 0; i < first.size(); i++) {
            assertFalse(first.get(i).equals(third.get(i)));
        }
    }

    @Test
    public void testDoubleReuse() {
        // as 2 is an arbitrary test for n>1 we stop here testing stuff like unlimited reuse
        matchers.forEach(matcher -> matcher.setTileReuseLimit(2));
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 5));
        List<String> first = getSources(1, 1, 0xFFFF0000);
        List<String> second = getSources(1, 1, 0xFFFF0000);
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 5));
        for (int i = 0; i < first.size(); i++) {
            assertTrue(first.get(i).equals(second.get(i)));
        }
        List<String> third = getSources(1, 1, 0xFFFF0000);
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 5));
        for (int i = 0; i < first.size(); i++) {
            assertTrue(first.get(i).equals(third.get(i)));
        }
        List<String> fourth = getSources(1, 1, 0xFFFF0000);
        assertTrue(matchers.stream().mapToInt(TileMatcher::getUsedTilesCount).allMatch(i-> i == 4));
        for (int i = 0; i < first.size(); i++) {
            assertFalse(first.get(i).equals(fourth.get(i)));
        }
    }
}
