package assembling;

import util.PercentProgressListener;

/**
 * Used to notify the listener about progress made (in percent) and allowing the listener to tell the assemblor to stop
 * working (as fast as possible).
 * Created by dd on 28.06.17.
 */
public interface ProgressCallback extends PercentProgressListener {
    boolean isCancelled();
}
