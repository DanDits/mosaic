package util;

import data.mosaic.MosaicTile;
import matching.MockTile;
import matching.workers.SimpleLinearTileMatcher;
import org.junit.Before;
import org.junit.Test;
import reconstruction.MosaicFragment;
import util.image.Color;
import util.image.ColorSpace;
import util.image.KDColorTree;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by dd on 22.06.17.
 */
public class KDColorTreeTest {

    private KDColorTree<MosaicTile<String>> tree;
    private ArrayList<MosaicTile<String>> tiles;
    private int amount;
    private ColorSpace space;

    @Before
    public void init() {
        space = ColorSpace.Brightness.INSTANCE_WITH_ALPHA;
        tiles = new ArrayList<>();
        amount = 10000;
        Random rnd = new Random(1337);
        for (int i = 0; i < amount - 3; i++) {
            tiles.add(new MockTile("S" + i,
                                   Color.rgb(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255)),
                                   5, 5));
        }
        tiles.add(new MockTile("D1", 0xFFAAAA00, 5, 5));
        tiles.add(new MockTile("D2", 0xFFAAAA11, 5, 5));
        tiles.add(new MockTile("D3", 0xFFAAAA55, 5, 5));
        rnd = new Random(1337);
        tree = KDColorTree.make(rnd, tiles, space);
    }

    @Test
    public void testCreation() {
        assertTrue(tree != null);
    }

    @Test
    public void testIteratorSize() {
        Set<MosaicTile<String>> data = new HashSet<>();
        for (KDColorTree.Node<MosaicTile<String>> node : tree) {
            data.add(node.getData());
        }
        assertEquals(amount, data.size());
    }

    @Test
    public void testRemoveElements() {
        for (MosaicTile<String> tile : tiles) {
            assertTrue(tree.removeNode(tile));
        }
    }

    @Test
    public void testNearestNeighborExact() {
        Optional<MosaicTile<String>> dataOpt;
        for (MosaicTile<String> tile : tiles) {
            dataOpt = tree.getNearestNeighbor(tile.getAverageARGB());
            assertTrue(dataOpt.isPresent());
            boolean sourceEqual = tile.getSource().equals(dataOpt.get().getSource());
            boolean colorEqual = space.getDistance(tile.getAverageARGB(), dataOpt.get().getAverageARGB()) < 1E-10;
            assertTrue(sourceEqual || colorEqual);
        }
    }

    @Test
    public void manyQueries() {
        int delta = 2;
        for (int red = 0; red <= 255; red += delta) {
            for (int green = 0; green <= 255; green += delta) {
                for (int blue = 0; blue <= 255; blue += delta) {
                    int color = Color.rgb(red, green, blue);
                    Optional<MosaicTile<String>> tile = tree.getNearestNeighbor(color);
                    assertTrue(tile.isPresent());
                }
            }
        }
    }

    @Test
    public void testCompareWithLinearMatcher() {
        SimpleLinearTileMatcher<String> matcher = new SimpleLinearTileMatcher<>(tiles, space);
        int delta = 10; // will take about 25 seconds for delta=2 as linear matcher is slow
        for (int red = 0; red <= 255; red += delta) {
            for (int green = 0; green <= 255; green += delta) {
                for (int blue = 0; blue <= 255; blue += delta) {
                    int color = Color.rgb(red, green, blue);
                    Optional<MosaicTile<String>> tile1 = matcher.calculateBestMatch(new MosaicFragment(5, 5, color));
                    assertTrue(tile1.isPresent());
                    Optional<MosaicTile<String>> tile2 = tree.getNearestNeighbor(color);
                    assertTrue(tile2.isPresent());
                    boolean sameSource = tile1.get().getSource().equals(tile2.get().getSource());
                    double dist1 = space.getDistance(tile1.get().getAverageARGB(), color);
                    double dist2 = space.getDistance(tile2.get().getAverageARGB(), color);
                    assertTrue(sameSource || dist1 == dist2);
                }
            }
        }
    }


}
