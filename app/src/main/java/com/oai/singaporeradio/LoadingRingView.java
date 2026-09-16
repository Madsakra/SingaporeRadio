package com.oai.singaporeradio;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.animation.LinearInterpolator;

/** Decorative loading feedback. The underlying playback control remains the touch target. */
@SuppressLint("ViewConstructor") // Created programmatically with a palette; never inflated from XML.
final class LoadingRingView extends View {
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private final int colour, trackColour;
    private final float inset, stroke;
    private final ValueAnimator animator;
    private boolean motionAllowed;
    private float angle;
    private final ContentObserver motionPreference = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) { updateMotion(); }
    };

    LoadingRingView(Context context, int colour, int trackColour, int strokeDp, int insetDp) {
        super(context);
        this.colour = colour;
        this.trackColour = trackColour;
        float density = getResources().getDisplayMetrics().density;
        stroke = strokeDp * density;
        inset = insetDp * density;
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(stroke);
        paint.setStrokeCap(Paint.Cap.ROUND);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setClickable(false);
        setFocusable(false);
        animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(2200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            angle = (float) animation.getAnimatedValue();
            invalidate();
        });
        setVisibility(INVISIBLE);
    }

    void setLoading(boolean loading) {
        // Invisible preserves the artwork and button geometry in every playback state.
        setVisibility(loading ? VISIBLE : INVISIBLE);
        updateMotion();
    }

    void setMotionAllowed(boolean allowed) {
        motionAllowed = allowed;
        updateMotion();
    }

    boolean isAnimating() { return animator.isRunning(); }

    private void updateMotion() {
        if (animator == null) return; // View can dispatch visibility while being constructed.
        boolean enabled = Settings.Global.getFloat(getContext().getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
        boolean shouldRun = motionAllowed && isAttachedToWindow() && isShown()
            && getWindowVisibility() == VISIBLE && enabled;
        if (shouldRun) {
            if (!animator.isStarted()) animator.start();
        } else {
            animator.cancel();
            angle = 0f;
            invalidate();
        }
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        getContext().getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, motionPreference);
        updateMotion();
    }

    @Override protected void onDetachedFromWindow() {
        getContext().getContentResolver().unregisterContentObserver(motionPreference);
        animator.cancel();
        super.onDetachedFromWindow();
    }

    @Override protected void onVisibilityChanged(View changedView, int visibility) {
        super.onVisibilityChanged(changedView, visibility);
        updateMotion();
    }

    @Override protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        updateMotion();
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float diameter = Math.min(getWidth(), getHeight()) - 2 * inset - stroke;
        float left = (getWidth() - diameter) / 2f, top = (getHeight() - diameter) / 2f;
        bounds.set(left, top, left + diameter, top + diameter);
        paint.setColor(trackColour);
        canvas.drawOval(bounds, paint);
        paint.setColor(colour);
        canvas.drawArc(bounds, angle - 90f, 80f, false, paint);
    }
}
