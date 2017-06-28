package util.caching;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by dd on 03.06.17.
 */
public class LruCache<K, V> extends LinkedHashMap<K, V> implements Cachable<K, V> {
    private static final int INITIAL_CAPACITY = 16;
    private static final float LOAD_FACTOR = 0.75f;
    private int cacheSize;

    public LruCache(int cacheSize) {
        super(INITIAL_CAPACITY, LOAD_FACTOR, true);
        this.cacheSize = cacheSize;
    }

    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() >= cacheSize;
    }

    @Override
    public void clearCache(int sizeHint) {
        clear();
    }

    @Override
    public Optional<V> getFromCache(K key) {
        return Optional.ofNullable(get(key));
    }

    @Override
    public void addToCache(K key, V value) {
        put(key, value);
    }

    @Override
    public void setCacheSize(int sizeHint) {
        cacheSize = sizeHint;
    }

    @Override
    public void removeFromCache(K key) {
        remove(key);
    }

    @Override
    public void removeValueFromCache(V value) {
        // we do a brute force search as we cannot even be sure the value is hashable
        keySet().removeIf(key -> get(key).equals(value));
    }
}