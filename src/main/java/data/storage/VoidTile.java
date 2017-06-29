package data.storage;

/**
 * Created by dd on 29.06.17.
 */
public class VoidTile implements MosaicTile<Void> {
    private final int color;

    public VoidTile(int color) {
        this.color = color;
    }
    @Override
    public Void getSource() {
        return null;
    }

    @Override
    public int getAverageARGB() {
        return color;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public int getHeight() {
        return 0;
    }
}
