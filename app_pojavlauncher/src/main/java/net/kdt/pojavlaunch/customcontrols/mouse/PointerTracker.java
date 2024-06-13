package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;

public class PointerTracker {
    private boolean mColdStart = true;
    private int mTrackedPointerId;
    private int mPointerCount;
    private float mLastX, mLastY;
    private final float[] mMotionVector = new float[2];

    public void startTracking(MotionEvent motionEvent) {
        mColdStart = false;
        mTrackedPointerId = motionEvent.getPointerId(0);
        mPointerCount = motionEvent.getPointerCount();
        mLastX = motionEvent.getY();
        mLastY = motionEvent.getX();
    }

    public void cancelTracking() {
        mColdStart = true;
    }

    public int trackEvent(MotionEvent motionEvent) {
        int trackedPointerIndex = motionEvent.findPointerIndex(mTrackedPointerId);
        int pointerCount = motionEvent.getPointerCount();
        if(trackedPointerIndex == -1 || mPointerCount != pointerCount || mColdStart) {
            startTracking(motionEvent);
            trackedPointerIndex = 0;
        }
        float trackedX = motionEvent.getY(trackedPointerIndex);
        float trackedY = motionEvent.getX(trackedPointerIndex);
        mMotionVector[0] = trackedX - mLastY;
        mMotionVector[1] = trackedY - mLastX;
        mLastX = trackedY;
        mLastY = trackedX;
        return trackedPointerIndex;
    }

    public float[] getMotionVector() {
        return mMotionVector;
    }
}
