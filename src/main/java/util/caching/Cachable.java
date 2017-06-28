package util.caching;

import java.util.Optional;
import java.util.function.Function;

/**
 * Created by dd on 05.06.17.
 */
public interface Cachable<K, V> {
    int CLEAR_EMPTY = 0;

    void clearCache(int sizeHint);

    default Optional<? extends V> getFromCache(K key, Function<K, Optional<? extends V>> creator) {
        Optional<? extends V> result = getFromCache(key);
        if (!result.isPresent()) {
            result = creator.apply(key);
        }
        result.ifPresent(v -> addToCache(key, v));
        return result;
    }

    Optional<? extends V> getFromCache(K key);

    void addToCache(K key, V value);

    void setCacheSize(int sizeHint);

    void removeFromCache(K key);

    void removeValueFromCache(V value);
}
