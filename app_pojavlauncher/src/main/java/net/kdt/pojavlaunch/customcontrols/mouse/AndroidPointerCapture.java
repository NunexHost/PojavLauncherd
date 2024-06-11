package net.kdt.pojavlaunch.customcontrols.mouse;

import android.os.Build;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.RequiresApi;

import net.kdt.pojavlaunch.MainActivity;
import net.kdt.pojavlaunch.MinecraftGLSurface;
import net.kdt.pojavlaunch.Tools;

import org.lwjgl.glfw.CallbackBridge;

@RequiresApi(api = Build.VERSION_CODES.O)
public class AndroidPointerCapture implements ViewTreeObserver.OnWindowFocusChangeListener, View.OnCapturedPointerListener {
    private static final float TOUCHPAD_SCROLL_THRESHOLD = 1;
    private final float mTouchpadScale; // Pre-calculated scaling factor
    private final AbstractTouchpad mTouchpad;
    private final View mHostView;
    private final Scroller mScroller; // Optional, remove if not essential

    public AndroidPointerCapture(AbstractTouchpad touchpad, View hostView, float scaleFactor) {
        this.mTouchpadScale = scaleFactor * scaleFactor; // Pre-calculate
        this.mTouchpad = touchpad;
        this.mHostView = hostView;
        mHostView.setOnCapturedPointerListener(this);
        mHostView.getViewTreeObserver().addOnWindowFocusChangeListener(this);
        mScroller = new Scroller(TOUCHPAD_SCROLL_THRESHOLD); // Optional
    }

    private void enableTouchpadIfNecessary() {
        if (!mTouchpad.getDisplayState()) mTouchpad.enable(true);
    }

    public void handleAutomaticCapture() {
        if (!CallbackBridge.isGrabbing()) return;
        if (mHostView.hasPointerCapture()) {
            enableTouchpadIfNecessary();
            return;
        }
        if (!mHostView.hasWindowFocus()) {
            mHostView.requestFocus();
        } else {
            mHostView.requestPointerCapture();
        }
    }

    @Override
    public boolean onCapturedPointer(View view, MotionEvent event) {
        boolean isGrabbing = CallbackBridge.isGrabbing();
        if (!isGrabbing) {
            enableTouchpadIfNecessary();
            float relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X); // Read from MotionEvent
            float relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y); // Read from MotionEvent
            if (event.getPointerCount() < 2) {
                mTouchpad.applyMotionVector(relX, relY);
                if (mScroller != null) { // Optional
                    mScroller.resetScrollOvershoot();
                }
            } else {
                if (mScroller != null) { // Optional
                    mScroller.performScroll(relX, relY);
                }
            }
        } else {
            CallbackBridge.mouseX += relX * mTouchpadScale;
            CallbackBridge.mouseY += relY * mTouchpadScale;
            CallbackBridge.sendCursorPos(CallbackBridge.mouseX, CallbackBridge.mouseY);
        }

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_MOVE:
                return true;
            case MotionEvent.ACTION_BUTTON_PRESS:
                return MinecraftGLSurface.sendMouseButtonUnconverted(event.getActionButton(), true);
            case MotionEvent.ACTION_BUTTON_RELEASE:
                return MinecraftGLSurface.sendMouseButtonUnconverted(event.getActionButton(), false);
            case MotionEvent.ACTION_SCROLL:
                CallbackBridge.sendScroll(
                        event.getAxisValue(MotionEvent.AXIS_HSCROLL),
                        event.getAxisValue(MotionEvent.AXIS_VSCROLL)
                );
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus && MainActivity.isAndroid8OrHigher()) mHostView.requestPointerCapture();
    }

    public void detach() {
        mHostView.setOnCapturedPointerListener(null);
        mHostView.getViewTreeObserver().removeOnWindowFocusChangeListener(this);
    }
}
