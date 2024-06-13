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

    public AndroidPointerCapture(AbstractTouchpad touchpad, View hostView, float scaleFactor) {
        this.mScaleFactor = scaleFactor;
        this.mTouchpad = touchpad;
        this.mHostView = hostView;
        hostView.setOnCapturedPointerListener(this);
        hostView.getViewTreeObserver().addOnWindowFocusChangeListener(this);
    }

    private void enableTouchpadIfNecessary() {
        if(!mTouchpad.getDisplayState()) mTouchpad.enable(true);
    }

    public void handleAutomaticCapture() {
        if(!CallbackBridge.isGrabbing()) return;
        if(mHostView.hasPointerCapture()) {
            enableTouchpadIfNecessary();
            return;
        }
        if(!mHostView.hasWindowFocus()) {
            mHostView.requestFocus();
        } else {
            mHostView.requestPointerCapture();
        }
    }

    @Override
    public boolean onCapturedPointer(View view, MotionEvent event) {
        float relX, relY;

        // Differentiate between touchpad and mouse events
        if (!CallbackBridge.isGrabbing()) {
            // Touchpad input
            relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X);
            relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y);

            enableTouchpadIfNecessary();

            // Handle scrolling gesture
            relX *= mMousePrescale;
            relY *= mMousePrescale;
            if(event.getPointerCount() < 2) {
                mTouchpad.applyMotionVector(relX, relY);
                mScroller.resetScrollOvershoot();
            } else {
                mScroller.performScroll(relX, relY);
            }
        } else {
            // OTG mouse input
            relX = event.getX() - event.getXPrecision();
            relY = event.getY() - event.getYPrecision();

            // Update cursor position and send to bridge
            CallbackBridge.mouseX += (relX * mScaleFactor);
            CallbackBridge.mouseY += (relY * mScaleFactor);
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
        if(hasFocus && MainActivity.isAndroid8OrHigher()) mHostView.requestPointerCapture();
    }

    public void detach() {
        mHostView.setOnCapturedPointerListener(null);
        mHostView.getViewTreeObserver().removeOnWindowFocusChangeListener(this);
    }
}
