package matching;

import data.image.AbstractColor;
import data.mosaic.MosaicTile;
import org.junit.Before;
import org.junit.Test;
import util.image.KDColorTree;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Created by dd on 22.06.17.
 */
public class KDColorTreeTest {

    private KDColorTree<MosaicTile<String>> tree;

    @Before
    public void init() {
        List<MosaicTile<String>> tiles = new ArrayList<>();
        int seed = 1337;
        int amount = 15;
        Random rnd = new Random(seed);
        for (int i = 0; i < amount; i++) {
            tiles.add(new MockTile("S" + i,
                                    AbstractColor.rgb(rnd.nextInt(255), rnd.nextInt(255), rnd.nextInt(255)),
                                    5, 5));
        }
        tree = KDColorTree.make(tiles, MosaicTile::getAverageARGB, false);
        System.err.println(this.tree);
    }

    @Test
    public void testCreation() {
        assertTrue(tree != null);
    }

    @Test
    public void testIterator() {
        Set<MosaicTile<String>> data = new HashSet<>();
        for (KDColorTree.Node<MosaicTile<String>> node : tree) {
            data.add(node.getData());
        }
        assertEquals(15, data.size());
    }
}
