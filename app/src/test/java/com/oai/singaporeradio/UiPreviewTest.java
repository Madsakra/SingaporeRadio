package com.oai.singaporeradio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
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
import org.robolectric.annotation.GraphicsMode;
import java.io.File;
import java.io.FileOutputStream;
import static org.junit.Assert.*;
import static com.oai.singaporeradio.MainActivityTest.*;

/** Actual native Android view renders, not design mockups. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiPreviewTest {
    @Before public void resetPreferences() {
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, 0).edit().clear().commit();
    }

    @Test public void renderApprovedFlow() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            capture(activity, "01-all-stations", 411, 891);
            for (int i = 0; i < 3; i++) list(activity).findViewWithTag("favourite:" + StationData.ALL.get(i).name).performClick();
            activity.findViewById(R.id.tab_favourites).performClick();
            capture(activity, "02-favourites", 411, 891);
            list(activity).findViewWithTag("play:LOVE 972").performClick();
            status(activity, "Playing LOVE 972");
            capture(activity, "03-focus-player", 411, 891);
            activity.findViewById(R.id.browse_stations).performClick();
            capture(activity, "04-playing-bar", 411, 891);
            activity.findViewById(R.id.mini_toggle).performClick();
            capture(activity, "05-stopped-bar", 411, 891);
            query(activity, "999");
            capture(activity, "06-no-results", 411, 891);
        }
    }

    @Test public void renderLoadingFeedback() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            showWindow(activity);
            list(activity).findViewWithTag("play:YES 933").performClick();
            advanceFrames(320);
            capture(activity, "11-connecting-player", 411, 891);
            for (int i = 0; i < 22; i++) {
                capture(activity, String.format(java.util.Locale.ROOT, "motion-player-%02d", i), 411, 891);
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(java.time.Duration.ofMillis(100));
            }
            activity.findViewById(R.id.browse_stations).performClick();
            advanceFrames(260);
            status(activity, "Buffering YES 933…");
            capture(activity, "12-buffering-bar", 411, 891);
            for (int i = 0; i < 22; i++) {
                capture(activity, String.format(java.util.Locale.ROOT, "motion-bar-%02d", i), 411, 891);
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper())
                    .idleFor(java.time.Duration.ofMillis(100));
            }
        }
    }

    @Test public void renderPlayerTransition() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            showWindow(activity);
            list(activity).findViewWithTag("play:YES 933").performClick();
            status(activity, "Playing YES 933");
            advanceFrames(320);
            activity.findViewById(R.id.browse_stations).performClick();
            advanceFrames(260);
            capture(activity, "transition-00-browser", 411, 891);
            activity.findViewById(R.id.open_player).performClick();
            for (int i = 0; i < 16; i++) {
                capture(activity, String.format(java.util.Locale.ROOT, "transition-01-open-%02d", i), 411, 891);
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(20));
            }
            capture(activity, "transition-02-player", 411, 891);
            activity.findViewById(R.id.browse_stations).performClick();
            for (int i = 0; i < 14; i++) {
                capture(activity, String.format(java.util.Locale.ROOT, "transition-03-close-%02d", i), 411, 891);
                org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(20));
            }
            capture(activity, "transition-04-browser", 411, 891);
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp-mdpi")
    public void renderSmallScreenAndLargeText() throws Exception {
        RuntimeEnvironment.setFontScale(1.6f);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            list(activity).findViewWithTag("play:LOVE 972").performClick();
            capture(activity, "07-large-text-player", 360, 640);
            activity.findViewById(R.id.browse_stations).performClick();
            capture(activity, "08-large-text-browser", 360, 640);
        }
    }

    @Test @Config(qualifiers = "w640dp-h360dp-land-mdpi")
    public void renderLandscapeError() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            list(activity).findViewWithTag("play:LOVE 972").performClick();
            activity.findViewById(R.id.browse_stations).performClick();
            status(activity, "No internet connection. Turn on Wi-Fi or mobile data, then tap PLAY.");
            capture(activity, "09-landscape-error", 640, 360);
        }
    }

    @Test public void renderLanguages() throws Exception {
        for (String tag : new String[]{"zh", "ms", "ta"}) {
            RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, 0).edit().putString("language", tag).commit();
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                MainActivity activity = controller.get();
                list(activity).findViewWithTag("play:LOVE 972").performClick();
                activity.findViewById(R.id.browse_stations).performClick();
                capture(activity, "10-language-" + tag, 411, 891);
            }
        }
    }

    private static void capture(MainActivity activity, String name, int width, int height) throws Exception {
        layout(activity, width, height);
        View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        if (name.equals("04-playing-bar")) {
            View toggle = activity.findViewById(R.id.mini_toggle);
            android.graphics.Rect bounds = new android.graphics.Rect(0, 0, toggle.getWidth(), toggle.getHeight());
            ((ViewGroup) root).offsetDescendantRectToMyCoords(toggle, bounds);
            int minX = bounds.right, maxX = bounds.left, minY = bounds.bottom, maxY = bounds.top;
            for (int y = bounds.top; y < bounds.bottom; y++) {
                for (int x = bounds.left; x < bounds.right; x++) {
                    if (bitmap.getPixel(x, y) == android.graphics.Color.WHITE) {
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                    }
                }
            }
            assertTrue("Stop symbol is visible", maxX > minX && maxY > minY);
            assertEquals("Stop symbol stays horizontally centred after opening the full player",
                bounds.exactCenterX(), (minX + maxX + 1) / 2f, 2f);
            assertEquals("Stop symbol stays vertically centred after opening the full player",
                bounds.exactCenterY(), (minY + maxY + 1) / 2f, 2f);
        }
        File directory = new File("build/reports/ui-redesign");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        bitmap.recycle();
    }
}
