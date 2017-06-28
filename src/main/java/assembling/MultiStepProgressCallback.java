package assembling;

import util.MultistepPercentProgressListener;

/**
 * Created by dd on 28.06.17.
 */
public class MultiStepProgressCallback extends MultistepPercentProgressListener implements ProgressCallback {

    private final ProgressCallback mCallback;

    public MultiStepProgressCallback(ProgressCallback callback, int steps) {
        super(callback, steps);
        mCallback = callback;
    }


    @Override
    public boolean isCancelled() {
        return mCallback.isCancelled();
    }
}
