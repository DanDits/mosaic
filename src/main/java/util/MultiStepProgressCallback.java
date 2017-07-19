package util;

/**
 * Created by dd on 28.06.17.
 */
public class MultiStepProgressCallback extends MultistepPercentProgressListener implements ProgressCallback {

    private final ProgressCallback mCallback;

    public MultiStepProgressCallback(ProgressCallback callback, int steps) {
        super(callback, steps);
        mCallback = callback;
    }

    public MultiStepProgressCallback(ProgressCallback callback, double[] weights) {
        super(callback, weights);
        mCallback = callback;
    }


    @Override
    public boolean isCancelled() {
        return mCallback.isCancelled();
    }
}
