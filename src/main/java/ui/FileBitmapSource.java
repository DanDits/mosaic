package ui;

import data.AbstractBitmap;
import data.AbstractBitmapFactory;
import data.BitmapSource;
import data.MosaicTile;
import util.caching.LruCache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by dd on 03.06.17.
 */
public class FileBitmapSource implements BitmapSource<String> {
    private LruCache<String, List<AbstractBitmap>> cache = new LruCache<>(100);

    @Override
    public AbstractBitmap getBitmap(MosaicTile<String> forTile, int requiredWidth, int requiredHeight) {
        List<AbstractBitmap> cached = cache.get(forTile.getSource());
        if (cached == null) {
            cached = new ArrayList<>();
            AbstractBitmap bitmap = AbstractBitmapFactory.makeInstance(new File(forTile.getSource())).createBitmap();
            cached.add(bitmap);
            cache.put(forTile.getSource(), cached);
        }
        for (AbstractBitmap bitmap : cached) {
            if (bitmap.getWidth() == requiredWidth && bitmap.getHeight() == requiredHeight) {
                return bitmap;
            }
        }
        AbstractBitmap bitmap = cached.get(0).obtainResized(requiredWidth, requiredHeight);
        cached.add(bitmap);
        return bitmap;
    }
}
