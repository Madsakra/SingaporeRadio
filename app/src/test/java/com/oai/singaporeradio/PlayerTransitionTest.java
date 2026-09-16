package com.oai.singaporeradio;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import static com.oai.singaporeradio.MainActivityTest.*;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24, 36}, qualifiers = "w411dp-h891dp-mdpi")
public class PlayerTransitionTest {
    @Before public void reset() {
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, 0).edit().clear().commit();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            "com.oai.singaporeradio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
        motionScale(1f);
    }

    @Test public void navigationAnimatesWithoutPlaybackCommandsAndPreservesTheBrowser() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = browserPlaying(controller);
            ScrollView scroller = activity.findViewById(R.id.station_scroller);
            scroller.scrollTo(0, 350);
            int scrollY = scroller.getScrollY();
            View player = activity.findViewById(R.id.focus_player);
            View browser = (View) scroller.getParent();
            View bar = activity.findViewById(R.id.mini_player);
            int barTop = bar.getTop();
            activity.findViewById(R.id.open_player).performClick();
            assertEquals(View.VISIBLE, shield(activity).getVisibility());
            assertEquals(0f, player.getAlpha(), 0.001f);
            assertEquals(32f, player.getTranslationY(), 0.001f);
            advance(100);
            assertTrue(player.getAlpha() > 0f && player.getAlpha() < 1f);
            assertTrue(player.getTranslationY() > 0f && player.getTranslationY() < 32f);
            assertEquals(1f, browser.getAlpha(), 0f);
            assertEquals(barTop, bar.getTop());
            assertEquals(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS, browser.getImportantForAccessibility());
            advance(220);
            assertSettled(activity, true);
            activity.findViewById(R.id.browse_stations).performClick();
            advance(80);
            assertTrue(player.getAlpha() < 1f && player.getAlpha() > 0f);
            advance(180);
            assertSettled(activity, false);
            assertEquals(scrollY, scroller.getScrollY());
            assertNull("Opening and closing never restart or stop audio", shadowOf(activity).getNextStartedService());
        }
    }

    @Test public void rapidBackAndReopenReverseFromTheCurrentPosition() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = browserPlaying(controller);
            View player = activity.findViewById(R.id.focus_player);
            activity.findViewById(R.id.open_player).performClick();
            advance(100);
            float alpha = player.getAlpha(), y = player.getTranslationY();
            activity.onBackPressed();
            assertEquals(alpha, player.getAlpha(), 0f);
            assertEquals(y, player.getTranslationY(), 0f);
            advance(64);
            alpha = player.getAlpha(); y = player.getTranslationY();
            activity.findViewById(R.id.open_player).performClick();
            assertEquals(alpha, player.getAlpha(), 0f);
            assertEquals(y, player.getTranslationY(), 0f);
            advance(320);
            assertSettled(activity, true);
            activity.onBackPressed();
            advance(260);
            assertSettled(activity, false);
            assertNull(shadowOf(activity).getNextStartedService());
        }
    }

    @Test public void reducedMotionAndBackgroundingSettleImmediately() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = browserPlaying(controller);
            motionScale(0f);
            activity.findViewById(R.id.open_player).performClick();
            assertSettled(activity, true);
            motionScale(1f);
            activity.onBackPressed();
            advance(64);
            assertEquals(View.VISIBLE, shield(activity).getVisibility());
            motionScale(0f);
            assertSettled(activity, false);
            motionScale(1f);
            activity.findViewById(R.id.open_player).performClick();
            advance(64);
            controller.pause();
            assertSettled(activity, true);
            controller.resume();
            advance(400);
            assertSettled(activity, true);
            activity.onBackPressed();
            controller.pause();
            assertSettled(activity, false);
            controller.resume();
        }
    }

    @Test public void recreationRestoresTheRequestedDestinationWithoutReplayingTheTransition() {
        for (boolean destination : new boolean[]{true, false}) {
            Bundle state = new Bundle();
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                MainActivity activity = browserPlaying(controller);
                activity.findViewById(R.id.open_player).performClick();
                advance(64);
                if (!destination) activity.onBackPressed();
                controller.saveInstanceState(state);
            }
            try (ActivityController<MainActivity> restored = Robolectric.buildActivity(MainActivity.class).setup(state)) {
                assertSettled(restored.get(), destination);
                assertNull(shadowOf(restored.get()).getNextStartedService());
            }
        }
    }

    @Test public void touchesCannotPassThroughTheMovingPlayerButStopWorksAfterItSettles() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = browserPlaying(controller);
            ViewGroup root = (ViewGroup) ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            View toggle = activity.findViewById(R.id.mini_toggle);
            Rect miniBounds = new Rect(0, 0, toggle.getWidth(), toggle.getHeight());
            root.offsetDescendantRectToMyCoords(toggle, miniBounds);
            activity.findViewById(R.id.open_player).performClick();
            tap(root, miniBounds.centerX(), miniBounds.centerY());
            assertNull("The old shortcut cannot receive a tap through the new screen", shadowOf(activity).getNextStartedService());
            advance(320);
            View stop = activity.findViewById(R.id.stop_button);
            Rect bounds = new Rect(0, 0, stop.getWidth(), stop.getHeight());
            root.offsetDescendantRectToMyCoords(stop, bounds);
            tap(root, bounds.centerX(), bounds.centerY());
            assertEquals(RadioService.ACTION_STOP, shadowOf(activity).getNextStartedService().getAction());
        }
    }

    private static MainActivity browserPlaying(ActivityController<MainActivity> controller) {
        MainActivity activity = controller.get();
        showWindow(activity);
        list(activity).findViewWithTag("play:YES 933").performClick();
        assertEquals(RadioService.ACTION_PLAY, shadowOf(activity).getNextStartedService().getAction());
        status(activity, "Playing YES 933");
        advance(320);
        activity.findViewById(R.id.browse_stations).performClick();
        advance(260);
        layout(activity, 411, 891);
        return activity;
    }

    private static void assertSettled(MainActivity activity, boolean open) {
        View player = activity.findViewById(R.id.focus_player);
        assertEquals(open ? View.VISIBLE : View.GONE, player.getVisibility());
        assertEquals(open ? View.GONE : View.VISIBLE, ((View) activity.findViewById(R.id.station_scroller).getParent()).getVisibility());
        assertEquals(1f, player.getAlpha(), 0f);
        assertEquals(0f, player.getTranslationY(), 0f);
        assertEquals(View.GONE, shield(activity).getVisibility());
    }

    private static View shield(MainActivity activity) { return activity.findViewById(R.id.player_transition_shield); }
    private static void advance(int millis) { advanceFrames(millis); }
    private static void motionScale(float scale) {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().getContentResolver(), Settings.Global.ANIMATOR_DURATION_SCALE, scale);
        advance(32);
    }

    private static void tap(View root, float x, float y) {
        long time = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, x, y, 0);
        MotionEvent up = MotionEvent.obtain(time, time + 20, MotionEvent.ACTION_UP, x, y, 0);
        assertTrue(root.dispatchTouchEvent(down));
        assertTrue(root.dispatchTouchEvent(up));
        down.recycle(); up.recycle();
        advance(32);
    }
}
