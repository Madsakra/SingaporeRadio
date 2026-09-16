package com.oai.singaporeradio;

import android.content.Intent;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
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
public class LoadingAnimationTest {
    @Before public void reset() {
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, 0).edit().clear().commit();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            "com.oai.singaporeradio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
        motionScale(1f);
    }

    @Test public void onlyTheVisibleLoadingPlayerAnimates() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            showWindow(activity);
            list(activity).findViewWithTag("play:LOVE 972").performClick();
            layout(activity, 411, 891);
            LoadingRingView full = activity.findViewById(R.id.player_loading);
            LoadingRingView mini = activity.findViewById(R.id.mini_loading);
            assertTrue("attached=" + full.isAttachedToWindow() + ", shown=" + full.isShown()
                + ", window=" + full.getWindowVisibility(), full.isAnimating());
            assertFalse(mini.isAnimating());
            activity.findViewById(R.id.browse_stations).performClick();
            layout(activity, 411, 891);
            assertFalse(full.isAnimating());
            assertTrue(mini.isAnimating());
            int height = activity.findViewById(R.id.mini_player).getHeight();

            for (String message : new String[]{"Playing LOVE 972", "Paused LOVE 972. Tap PLAY to resume.",
                    "Unable to play LOVE 972.", "No internet connection.", "Connection timed out.", "Stopped"}) {
                status(activity, message);
                layout(activity, 411, 891);
                assertEquals(message, View.INVISIBLE, mini.getVisibility());
                assertFalse(message, mini.isAnimating());
                assertFalse(message, full.isAnimating());
                assertEquals("The bar never changes size", height, activity.findViewById(R.id.mini_player).getHeight());
            }
            status(activity, "Buffering LOVE 972…");
            assertTrue(mini.isAnimating());
            activity.findViewById(R.id.open_player).performClick();
            assertTrue(full.isAnimating());
            assertFalse(mini.isAnimating());
        }
    }

    @Test public void ringDoesNotInterceptStopWhileConnectingOrBuffering() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            showWindow(activity);
            list(activity).findViewWithTag("play:YES 933").performClick();
            assertEquals(RadioService.ACTION_PLAY, shadowOf(activity).getNextStartedService().getAction());
            activity.findViewById(R.id.browse_stations).performClick();
            advanceFrames(300);
            for (String message : new String[]{"Connecting to YES 933…", "Buffering YES 933…"}) {
                status(activity, message);
                layout(activity, 411, 891);
                View button = activity.findViewById(R.id.mini_toggle);
                ViewGroup shortcut = (ViewGroup) button.getParent();
                long time = SystemClock.uptimeMillis();
                // Dispatch through the overlay's parent, rather than bypassing hit testing with performClick.
                MotionEvent down = MotionEvent.obtain(time, time, MotionEvent.ACTION_DOWN, 28, 28, 0);
                MotionEvent up = MotionEvent.obtain(time, time + 30, MotionEvent.ACTION_UP, 28, 28, 0);
                assertTrue(shortcut.dispatchTouchEvent(down));
                assertTrue(shortcut.dispatchTouchEvent(up));
                down.recycle(); up.recycle();
                shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32));
                Intent stop = shadowOf(activity).getNextStartedService();
                assertNotNull(stop);
                assertEquals(RadioService.ACTION_STOP, stop.getAction());
                assertFalse(((LoadingRingView) activity.findViewById(R.id.mini_loading)).isAnimating());
                assertEquals(View.GONE, activity.findViewById(R.id.focus_player).getVisibility());
                assertTrue(button.getContentDescription().toString().startsWith("Play"));
            }
        }
    }

    @Test public void reducedMotionAndActivityLifecycleStopAnimation() {
        LoadingRingView ring;
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            showWindow(activity);
            list(activity).findViewWithTag("play:LOVE 972").performClick();
            ring = activity.findViewById(R.id.player_loading);
            layout(activity, 411, 891);
            assertTrue("attached=" + ring.isAttachedToWindow() + ", shown=" + ring.isShown()
                + ", window=" + ring.getWindowVisibility(), ring.isAnimating());
            motionScale(0f);
            assertFalse(ring.isAnimating());
            assertEquals("Loading remains visible without motion", View.VISIBLE, ring.getVisibility());
            motionScale(1f);
            assertTrue(ring.isAnimating());
            controller.pause();
            assertFalse(ring.isAnimating());
            controller.resume();
            assertTrue(ring.isAnimating());
        }
        assertFalse(ring.isAnimating());
    }

    private static void motionScale(float scale) {
        Settings.Global.putFloat(RuntimeEnvironment.getApplication().getContentResolver(),
            Settings.Global.ANIMATOR_DURATION_SCALE, scale);
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32));
    }
}
