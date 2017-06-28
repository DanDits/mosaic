package ui;

import data.storage.MosaicTile;

/**
 * Created by dd on 03.06.17.
 */
public class FileMosaicTile implements MosaicTile<String> {

    private int averageARGB;
    private String path;
    private int width;
    private int height;


    FileMosaicTile(String path, int averageARGB, int width, int height) {
        this.path = path;
        this.averageARGB = averageARGB;
        this.width = width;
        this.height = height;
    }

    @Override
    public String getSource() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileMosaicTile that = (FileMosaicTile) o;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public int getAverageARGB() {
        return averageARGB;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }
}
