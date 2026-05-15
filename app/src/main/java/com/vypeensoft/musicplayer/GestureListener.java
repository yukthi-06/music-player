package com.vypeensoft.musicplayer;

import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Custom gesture listener to detect left and right swipes.
 */
public class GestureListener extends GestureDetector.SimpleOnGestureListener {
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    private final SwipeCallback callback;

    public interface SwipeCallback {
        void onSwipeLeft();
        void onSwipeRight();
    }

    public GestureListener(SwipeCallback callback) {
        this.callback = callback;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e1 == null || e2 == null) return false;
        
        float diffX = e2.getX() - e1.getX();
        float diffY = e2.getY() - e1.getY();
        
        if (Math.abs(diffX) > Math.abs(diffY)) {
            if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffX > 0) {
                    callback.onSwipeRight();
                } else {
                    callback.onSwipeLeft();
                }
                return true;
            }
        }
        return false;
    }
}
