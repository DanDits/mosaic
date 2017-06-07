package ui;

import data.MosaicTile;
import data.storage.JSONStorage;
import data.storage.TileBuilder;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicJSONBuilder extends TileBuilder<String> {
    private static final FileMosaicJSONBuilder INSTANCE = new FileMosaicJSONBuilder();

    @Override
    public MosaicTile<String> makeTile(String source, int averageARGB) {
        return new FileMosaicTile(source, averageARGB);
    }

    public static Set<MosaicTile<String>> loadExistingTiles(File saveFile) {
        Set<MosaicTile<String>> tiles = new JSONStorage<String>().loadFromJSON(saveFile, INSTANCE);
        tiles = tiles.stream().filter(tile -> new File(tile.getSource()).exists())
                .collect(Collectors.toSet());
        return tiles;
    }
}
