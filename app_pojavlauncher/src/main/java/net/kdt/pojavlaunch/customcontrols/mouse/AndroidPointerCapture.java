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
    private static final float TOUCHPAD_SCROLL_THRESHOLD = 1; // Minimum movement required for scroll registration
    private final AbstractTouchpad mTouchpad; // Reference to the touchpad object
    private final View mHostView; // Reference to the host view
    private final float mScaleFactor; // Scaling factor for mouse movement
    private final float mMousePrescale = Tools.dpToPx(1); // Pre-scaling factor for mouse events
    private final Scroller mScroller = new Scroller(TOUCHPAD_SCROLL_THRESHOLD); // Scroller object for handling touchpad scrolling

    public AndroidPointerCapture(AbstractTouchpad touchpad, View hostView, float scaleFactor) {
        this.mScaleFactor = scaleFactor;
        this.mTouchpad = touchpad;
        this.mHostView = hostView;

        // Set the host view as the captured pointer listener and window focus change listener
        mHostView.setOnCapturedPointerListener(this);
        mHostView.getViewTreeObserver().addOnWindowFocusChangeListener(this);
    }

    private void enableTouchpadIfNecessary() {
        if (!mTouchpad.getDisplayState()) {
            mTouchpad.enable(true); // Enable the touchpad if it's not already enabled
        }
    }

    public void handleAutomaticCapture() {
        if (!CallbackBridge.isGrabbing()) {
            return; // If the application doesn't have control, do nothing
        }

        if (mHostView.hasPointerCapture()) {
            enableTouchpadIfNecessary(); // Enable touchpad if mouse has capture
            return;
        }

        if (!mHostView.hasWindowFocus()) {
            mHostView.requestFocus(); // Request focus if no focus
        } else {
            mHostView.requestPointerCapture(); // Request capture if has focus
        }
    }

    @Override
    public boolean onCapturedPointer(View view, MotionEvent event) {
        // Read relative movement from the event
        float relX = event.getAxisValue(MotionEvent.AXIS_RELATIVE_X);
        float relY = event.getAxisValue(MotionEvent.AXIS_RELATIVE_Y);

        // Check if the application has control (using CallbackBridge.isGrabbing())
        if (!CallbackBridge.isGrabbing()) {
            enableTouchpadIfNecessary(); // Enable touchpad if no control

            // Handle scrolling for multi-touch events (touchpad)
            if (event.getPointerCount() >= 2) {
                relX *= mMousePrescale;
                relY *= mMousePrescale;
                mScroller.performScroll(relX, relY);
            } else {
                // Handle single-touch events (touchpad or mouse)
                relX *= mMousePrescale;
                relY *= mMousePrescale;
                mTouchpad.applyMotionVector(relX, relY);
                mScroller.resetScrollOvershoot();
            }
        } else {
            // Handle mouse movement (direct position updates)
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
        } else { // Touchscreen behavior
            relX *= mMousePrescale;
            relY *= mMousePrescale;

            if(event.getPointerCount() < 2) {
                mTouchpad.applyMotionVector(relX, relY);
                mScroller.resetScrollOvershoot();
            } else {
                mScroller.performScroll(relX, relY);
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
