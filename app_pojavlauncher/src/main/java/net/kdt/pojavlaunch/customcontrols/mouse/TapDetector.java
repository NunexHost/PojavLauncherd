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
    private final static int TAP_SLOP_SQUARE = TAP_SLOP_SQUARE_PX; // Pre-calculate square

    private final int mTapNumberToDetect;
    private int mCurrentTapNumber = 0;

    private final int mDetectionMethod;

    private long mLastEventTime = 0;
    private float mLastX = 9999;
    private float mLastY = 9999;

    public TapDetector(int tapNumberToDetect, int detectionMethod) {
        this.mDetectionMethod = detectionMethod;
        this.mTapNumberToDetect = detectBothTouch() ? 2 * tapNumberToDetect : tapNumberToDetect;
    }

    public boolean onTouchEvent(MotionEvent e) {
        int eventAction = e.getActionMasked();
        int pointerIndex = getPointerIndex(eventAction);

        if (pointerIndex == -1 || mCurrentTapNumber == 0) {
            return false; // Useless event or no ongoing detection
        }

        float eventX = e.getX(pointerIndex);
        float eventY = e.getY(pointerIndex);
        long eventTime = e.getEventTime();

        long deltaTime = eventTime - mLastEventTime;
        int deltaX = (int) mLastX - (int) eventX;
        int deltaY = (int) mLastY - (int) eventY;

        mLastEventTime = eventTime;
        mLastX = eventX;
        mLastY = eventY;

        if ((deltaTime < TAP_MIN_DELTA_MS || deltaTime > TAP_MAX_DELTA_MS) ||
                (deltaX * deltaX + deltaY * deltaY > TAP_SLOP_SQUARE)) {
            if (mDetectionMethod == DETECTION_METHOD_BOTH && (eventAction == ACTION_UP || eventAction == ACTION_POINTER_UP)) {
                resetTapDetectionState();
                return false;
            } else {
                mCurrentTapNumber = 0;
            }
        }

        mCurrentTapNumber++;
        if (mCurrentTapNumber >= mTapNumberToDetect) {
            resetTapDetectionState();
            return true;
        }

        return false;
    }

    private int getPointerIndex(int eventAction) {
        if (detectDownTouch()) {
            return eventAction == ACTION_DOWN ? 0 : e.getActionIndex();
        } else if (detectUpTouch()) {
            return eventAction == ACTION_UP ? 0 : e.getActionIndex();
        }
        return -1;
    }

    private boolean isDetectionMethod(int method) {
        return (mDetectionMethod & method) == method;
    }

    private void resetTapDetectionState() {
        mCurrentTapNumber = 0;
        mLastEventTime = 0;
        mLastX = 9999;
        mLastY = 9999;
    }

    private boolean detectDownTouch() {
        return isDetectionMethod(DETECTION_METHOD_DOWN);
    }

    private boolean detectUpTouch() {
        return isDetectionMethod(DETECTION_METHOD_UP);
    }

    private boolean detectBothTouch() {
        return mDetectionMethod == DETECTION_METHOD_BOTH;
    }
}
