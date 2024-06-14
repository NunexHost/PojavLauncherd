package net.kdt.pojavlaunch.customcontrols.mouse;

import android.view.MotionEvent;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;

import net.kdt.pojavlaunch.Tools;

/**
 * Class aiming at better detecting X-tap events regardless of the POINTERS
 * Only uses the least amount of events possible,
 * since we aren't guaranteed to have all events in order
 */
public class TapDetector {

    public final static int DETECTION_METHOD_DOWN = 0x1;
    public final static int DETECTION_METHOD_UP = 0x2;
    public final static int DETECTION_METHOD_BOTH = 0x3; //Unused for now

    private final static int TAP_MIN_DELTA_MS = 10;
    private final static int TAP_MAX_DELTA_MS = 300;
    private final static int TAP_SLOP_SQUARE = (int) Math.pow(Tools.dpToPx(25), 2); // Pre-calculate square

    private final int mTapNumberToDetect;
    private int mCurrentTapNumber = 0;

    private final int mDetectionMethod;

    private long mLastEventTime = 0;
    private float mLastX = 9999;
    private float mLastY = 9999;

    public TapDetector(int tapNumberToDetect, int detectionMethod) {
        this.mDetectionMethod = detectionMethod;
        this.mTapNumberToDetect = (detectionMethod & DETECTION_METHOD_BOTH) != 0 ? 2 * tapNumberToDetect : tapNumberToDetect;
    }

    public boolean onTouchEvent(MotionEvent e) {
        int eventAction = e.getActionMasked();
        int pointerIndex = eventAction == ACTION_DOWN ? 0 : (eventAction == ACTION_POINTER_DOWN ? e.getActionIndex() : -1);

        if (pointerIndex == -1) return false;

        float eventX = e.getX(pointerIndex);
        float eventY = e.getY(pointerIndex);
        long eventTime = e.getEventTime();

        long deltaTime = eventTime - mLastEventTime;
        int deltaX = (int) (mLastX - eventX);
        int deltaY = (int) (mLastY - eventY);

        if ((deltaTime < TAP_MIN_DELTA_MS || deltaTime > TAP_MAX_DELTA_MS) || (deltaX * deltaX + deltaY * deltaY > TAP_SLOP_SQUARE)) {
            if ((mDetectionMethod & DETECTION_METHOD_BOTH) != 0 && (eventAction == ACTION_UP || eventAction == ACTION_POINTER_UP)) {
                clearTapState();
                return false;
            } else {
                mCurrentTapNumber = 0;
            }
        }

        mCurrentTapNumber++;
        if (mCurrentTapNumber >= mTapNumberToDetect) {
            clearTapState();
            return true;
        }

        mLastEventTime = eventTime;
        mLastX = eventX;
        mLastY = eventY;

        return false;
    }

    private void clearTapState() {
        mCurrentTapNumber = 0;
        mLastEventTime = 0;
        mLastX = 9999;
        mLastY = 9999;
    }

    private boolean isDetectionMethod(int method) {
        return (mDetectionMethod & method) != 0;
    }
}
