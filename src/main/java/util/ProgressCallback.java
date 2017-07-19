package util;

/**
 * Used to notify the listener about progress made (in percent) and allowing the listener to tell to stop
 * working (as fast as possible).
 * Created by dd on 28.06.17.
 */
public interface ProgressCallback extends PercentProgressListener {
    boolean isCancelled();
}
