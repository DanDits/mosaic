package ui;

import data.storage.JSONStorage;
import data.storage.MosaicTile;
import data.storage.TileBuilder;
import util.PercentProgressListener;

import java.io.File;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicJSONBuilder extends TileBuilder<String> {
    private static final FileMosaicJSONBuilder INSTANCE = new FileMosaicJSONBuilder();

    @Override
    public MosaicTile<String> makeTile(String source, int averageARGB, int width, int height) {
        return new FileMosaicTile(source, averageARGB, width, height);
    }

    public static Set<MosaicTile<String>> loadExistingTiles(File saveFile, PercentProgressListener updater) {
        Set<MosaicTile<String>> tiles = new JSONStorage<String>().loadFromJSON(saveFile, INSTANCE, updater);
        tiles = tiles.stream().filter(tile -> new File(tile.getSource()).exists())
                .collect(Collectors.toSet());
        return tiles;
    }
}
