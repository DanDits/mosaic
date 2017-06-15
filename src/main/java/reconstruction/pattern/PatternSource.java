package reconstruction.pattern;


import data.image.AbstractBitmap;
import data.image.AbstractBitmapFactory;
import data.image.BitmapSource;
import data.mosaic.MosaicTile;
import util.caching.LruCache;
import util.image.ColorAnalysisUtil;

/**
 * Created by daniel on 05.12.15.
 */
public abstract class PatternSource<S> implements BitmapSource<S> {

    private static final int DEFAULT_CACHE_SIZE = 10;
    private final LruCache<Integer, AbstractBitmap> mCache;

    public PatternSource() {
        mCache = new LruCache<>(getCacheSizeHint());
    }

    public int getCacheSizeHint() {
        return DEFAULT_CACHE_SIZE;
    }

    public AbstractBitmap getBitmap(MosaicTile<S> forTile, int requiredWidth, int requiredHeight) {
        int keyColor = forTile.getAverageARGB();
        return makePattern(keyColor, obtainBitmap(keyColor, requiredWidth, requiredHeight));
    }

    protected abstract AbstractBitmap makePattern(int color, AbstractBitmap base);

    protected AbstractBitmap obtainBitmap(int key, int width, int height) {
        AbstractBitmap cached = mCache.get(key);
        if (cached != null && cached.getWidth() == width && cached.getHeight() == height) {
            return cached;
        }
        cached = AbstractBitmapFactory.makeInstance(width, height).createBitmap();
        mCache.put(key, cached);
        return cached;
    }
}
