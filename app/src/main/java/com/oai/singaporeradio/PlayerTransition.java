package com.oai.singaporeradio;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.PathInterpolator;

/** Moves only the player surface; navigation never issues playback commands. */
final class PlayerTransition {
    private final ViewGroup browser, player;
    private final View touchShield;
    private final int browserFocusability, playerFocusability;
    private final float distance;
    private boolean open, resumed;
    private ValueAnimator animator;
    private final ContentObserver motionPreference = new ContentObserver(new Handler(Looper.getMainLooper())) {
        @Override public void onChange(boolean selfChange) {
            if (!motionEnabled()) finish();
        }
    };

    PlayerTransition(ViewGroup browser, ViewGroup player, View touchShield) {
        this.browser = browser;
        this.player = player;
        this.touchShield = touchShield;
        browserFocusability = browser.getDescendantFocusability();
        playerFocusability = player.getDescendantFocusability();
        distance = 32f * player.getResources().getDisplayMetrics().density;
    }

    void show(boolean showPlayer) {
        if (open == showPlayer && animator != null) return;
        boolean changed = open != showPlayer;
        open = showPlayer;
        cancelAnimation();
        boolean canAnimate = resumed && player.isAttachedToWindow()
            && player.getWindowVisibility() == View.VISIBLE
            && ((View) player.getParent()).getWidth() > 0 && motionEnabled();
        if (!changed || !canAnimate) {
            finish();
            return;
        }

        float fromAlpha = player.getVisibility() == View.VISIBLE ? player.getAlpha() : 0f;
        float fromY = player.getVisibility() == View.VISIBLE ? player.getTranslationY() : distance;
        float toAlpha = open ? 1f : 0f;
        float toY = open ? 0f : distance;
        player.setAlpha(fromAlpha);
        player.setTranslationY(fromY);
        browser.setVisibility(View.VISIBLE);
        player.setVisibility(View.VISIBLE);
        setInteractionState();
        // Neither surface should receive an accidental tap through the fading layer.
        touchShield.setVisibility(View.VISIBLE);
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(open ? 280 : 224);
        animator.setInterpolator(new PathInterpolator(0.2f, 0f, 0f, 1f));
        animator.addUpdateListener(value -> {
            float fraction = (float) value.getAnimatedValue();
            player.setAlpha(fromAlpha + (toAlpha - fromAlpha) * fraction);
            player.setTranslationY(fromY + (toY - fromY) * fraction);
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) { finish(); }
        });
        animator.start();
    }

    void onResume() {
        if (resumed) return;
        resumed = true;
        player.getContext().getContentResolver().registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, motionPreference);
    }

    void onPause() {
        if (resumed) player.getContext().getContentResolver().unregisterContentObserver(motionPreference);
        resumed = false;
        finish();
    }

    private boolean motionEnabled() {
        return Settings.Global.getFloat(player.getContext().getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f;
    }

    private void cancelAnimation() {
        if (animator == null) return;
        // A cancelled animation must not run the previous destination's completion callback.
        animator.removeAllListeners();
        animator.removeAllUpdateListeners();
        animator.cancel();
        animator = null;
    }

    private void finish() {
        cancelAnimation();
        player.setAlpha(1f);
        player.setTranslationY(0f);
        player.setVisibility(open ? View.VISIBLE : View.GONE);
        browser.setVisibility(open ? View.GONE : View.VISIBLE);
        touchShield.setVisibility(View.GONE);
        setInteractionState();
    }

    private void setInteractionState() {
        browser.setImportantForAccessibility(open ? View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            : View.IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        player.setImportantForAccessibility(open ? View.IMPORTANT_FOR_ACCESSIBILITY_AUTO
            : View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        browser.setDescendantFocusability(open ? ViewGroup.FOCUS_BLOCK_DESCENDANTS : browserFocusability);
        player.setDescendantFocusability(open ? playerFocusability : ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        if (open) browser.clearFocus(); else player.clearFocus();
    }
}
