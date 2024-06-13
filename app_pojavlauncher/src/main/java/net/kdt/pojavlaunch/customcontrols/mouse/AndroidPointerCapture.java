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
    private final AbstractTouchpad mTouchpad;
    private final View mHostView;
    private final float mScaleFactor;
    private final float mMousePrescale = Tools.dpToPx(1);
    private final Scroller mScroller = new Scroller(TOUCHPAD_SCROLL_THRESHOLD);

    // Variables to store previous touch position
    private float prevX = 0;
    private float prevY = 0;

    public AndroidPointerCapture(AbstractTouchpad touchpad, View hostView, float scaleFactor) {
        this.mScaleFactor = scaleFactor;
        this.mTouchpad = touchpad;
        this.mHostView = hostView;
        hostView.setOnCapturedPointerListener(this);
        hostView.getViewTreeObserver().addOnWindowFocusChangeListener(this);
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
        // Calculate displacement (delta) instead of using AXIS_RELATIVE_X/Y
        float curX = event.getX();
        float curY = event.getY();
        float deltaX = curX - prevX;
        float deltaY = curY - prevY;

        // Update previous position for next event
        prevX = curX;
        prevY = curY;

        if (!CallbackBridge.isGrabbing()) {
            enableTouchpadIfNecessary();
            // Handle scrolling gesture
            deltaX *= mMousePrescale;
            deltaY *= mMousePrescale;
            if (event.getPointerCount() < 2) {
                mTouchpad.applyMotionVector(deltaX, deltaY);
                mScroller.resetScrollOvershoot();
            } else {
                mScroller.performScroll(deltaX, deltaY);
            }
        } else {
            // Update cursor position with displacement and scaling
            CallbackBridge.mouseX += (deltaX * mScaleFactor);
            CallbackBridge.mouseY += (deltaY * mScaleFactor);
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
