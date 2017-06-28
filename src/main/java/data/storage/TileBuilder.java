package data.storage;

/**
 * Created by dd on 03.06.17.
 */
public abstract class TileBuilder<S> {
    public abstract MosaicTile<S> makeTile(String source, int averageARGB, int width, int height);
}
