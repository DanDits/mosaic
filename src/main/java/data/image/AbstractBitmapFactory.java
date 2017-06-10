package data.image;

import awt.BitmapFactory;

import java.io.File;

/**
 * Created by dd on 03.06.17.
 */
public abstract class AbstractBitmapFactory {

    protected int width;
    protected int height;
    protected File file;

    public static AbstractBitmapFactory makeInstance(int width, int height) {
        return new BitmapFactory(width, height);
    }
    public static AbstractBitmapFactory makeInstance(File file) {
        return new BitmapFactory(file);
    }

    public AbstractBitmapFactory(int width, int height) {
        checkDimension(width, height);
        this.width = width;
        this.height = height;
    }

    public AbstractBitmapFactory(File file) {
        checkFile(file);
        this.file = file;
    }

    public abstract AbstractBitmap createBitmap();

    private static void checkFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Not a valid image file:" + (file == null ? "NULL" : file.getAbsolutePath()));
        }
    }

    private static void checkDimension(int width, int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Illegal dimensions width/height:" + width + "/" + height);
        }
    }
}
