package com.oai.singaporeradio;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowAlertDialog;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24, 36}, qualifiers = "w411dp-h891dp-mdpi")
public class MainActivityTest {
    @Before public void resetPreferencesAndGrantReceiverPermission() {
        RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, 0).edit().clear().commit();
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            "com.oai.singaporeradio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
    }

    @Test public void allStationsStartTheirExistingStreamsAndOpenTheFocusPlayer() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            assertEquals(26, list(activity).getChildCount());
            assertEquals(View.GONE, activity.findViewById(R.id.mini_player).getVisibility());
            assertNull(shadowOf(activity).getNextStartedService());
            for (Station station : StationData.ALL) {
                View row = list(activity).findViewWithTag(station.name);
                assertNotNull(station.name, row);
                assertTrue(containsText((ViewGroup) row, station.name));
                assertTrue(containsText((ViewGroup) row, StationPresentation.subtitle(activity, station)));
                row.findViewWithTag("play:" + station.name).performClick();
                assertPlayIntent(activity, station);
                assertEquals(View.VISIBLE, activity.findViewById(R.id.focus_player).getVisibility());
                activity.findViewById(R.id.browse_stations).performClick();
                assertEquals(View.GONE, activity.findViewById(R.id.focus_player).getVisibility());
                assertNull("Browsing does not send playback commands", shadowOf(activity).getNextStartedService());
            }
        }
    }

    @Test public void favouritesPersistWithoutChangingPlaybackAndTabsRemainIndependentOfSearch() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            list(activity).findViewWithTag("favourite:LOVE 972").performClick();
            activity.findViewById(R.id.tab_favourites).performClick();
            assertEquals(1, list(activity).getChildCount());
            assertEquals("LOVE 972", list(activity).getChildAt(0).getTag());
            query(activity, "93.3");
            assertEquals(0, list(activity).getChildCount());
            activity.findViewById(R.id.tab_all).performClick();
            assertEquals(1, list(activity).getChildCount());
            assertEquals("YES 933", list(activity).getChildAt(0).getTag());
            assertNull(shadowOf(activity).getNextStartedService());
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            assertEquals(26, list(controller.get()).getChildCount()); // All stations is the launch default.
            controller.get().findViewById(R.id.tab_favourites).performClick();
            assertEquals(1, list(controller.get()).getChildCount());
            list(controller.get()).findViewWithTag("favourite:LOVE 972").performClick();
            assertEquals(0, list(controller.get()).getChildCount());
        }
    }

    @Test public void filterAndQuerySurviveRecreationAndNeverControlPlayback() {
        Bundle state = new Bundle();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.findViewById(R.id.station_filter).performClick();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            assertEquals(7, dialog.getListView().getCount());
            dialog.getListView().performItemClick(null, 3, 3); // Malay.
            assertEquals(2, list(activity).getChildCount());
            query(activity, "warNa  942");
            assertEquals(1, list(activity).getChildCount());
            assertEquals("WARNA 942", list(activity).getChildAt(0).getTag());
            controller.saveInstanceState(state);
            assertNull(shadowOf(activity).getNextStartedService());
        }
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup(state)) {
            assertEquals(1, list(controller.get()).getChildCount());
            assertEquals("WARNA 942", list(controller.get()).getChildAt(0).getTag());
            assertEquals("warNa  942", ((EditText) controller.get().findViewById(R.id.station_search)).getText().toString());
        }
    }

    @Test public void miniShortcutStopsAndRestartsWithoutOpeningPlayerOrChangingStation() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            Station station = StationData.ALL.get(1);
            list(activity).findViewWithTag("play:" + station.name).performClick();
            assertPlayIntent(activity, station);
            status(activity, "Playing LOVE 972");
            activity.findViewById(R.id.browse_stations).performClick();
            View toggle = activity.findViewById(R.id.mini_toggle);
            assertEquals("Stop LOVE 972", toggle.getContentDescription());
            toggle.performClick();
            assertEquals(RadioService.ACTION_STOP, shadowOf(activity).getNextStartedService().getAction());
            assertEquals(View.VISIBLE, activity.findViewById(R.id.mini_player).getVisibility());
            assertEquals("Play LOVE 972", toggle.getContentDescription());
            assertEquals(View.GONE, activity.findViewById(R.id.focus_player).getVisibility());
            toggle.performClick();
            assertPlayIntent(activity, station);
            assertEquals(View.GONE, activity.findViewById(R.id.focus_player).getVisibility());
            activity.findViewById(R.id.open_player).performClick();
            assertEquals(View.VISIBLE, activity.findViewById(R.id.focus_player).getVisibility());
            assertNull(shadowOf(activity).getNextStartedService());
            activity.onBackPressed();
            assertEquals(View.GONE, activity.findViewById(R.id.focus_player).getVisibility());
            assertNull(shadowOf(activity).getNextStartedService());
        }
    }

    @Test public void nextAndPreviousUseCapturedListEvenWhenBrowsingFiltersChange() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            for (int i = 0; i < 3; i++) list(activity).findViewWithTag("favourite:" + StationData.ALL.get(i).name).performClick();
            activity.findViewById(R.id.tab_favourites).performClick();
            list(activity).findViewWithTag("play:LOVE 972").performClick();
            assertPlayIntent(activity, StationData.ALL.get(1));
            activity.findViewById(R.id.browse_stations).performClick();
            activity.findViewById(R.id.tab_all).performClick();
            activity.setStationFilter(3);
            activity.findViewById(R.id.open_player).performClick();
            activity.findViewById(R.id.next_station).performClick();
            assertPlayIntent(activity, StationData.ALL.get(2));
            assertEquals("Favourites · 3 of 3", ((TextView) activity.findViewById(R.id.queue_position)).getText().toString());
            activity.findViewById(R.id.next_station).performClick();
            assertPlayIntent(activity, StationData.ALL.get(0));
            activity.findViewById(R.id.previous_station).performClick();
            assertPlayIntent(activity, StationData.ALL.get(2));
        }
    }

    @Test public void connectingBufferingPausedAndFailureExposeTheRightAction() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            list(activity).findViewWithTag("play:POWER 98 EDM Club Hits").performClick();
            assertPlayIntent(activity, StationPresentation.named("POWER 98 EDM Club Hits"));
            for (String message : new String[]{"Connecting to POWER 98 EDM Club Hits…", "Buffering POWER 98 EDM Club Hits…", "Playing POWER 98 EDM Club Hits"}) {
                status(activity, message);
                assertEquals("Stop POWER 98 EDM Club Hits", activity.findViewById(R.id.mini_toggle).getContentDescription());
            }
            for (String message : new String[]{"Paused POWER 98 EDM Club Hits. Tap PLAY to resume.",
                    "No internet connection. Turn on Wi-Fi or mobile data, then tap PLAY.",
                    "Unable to play POWER 98 EDM Club Hits. The stream may be unavailable or region-restricted. Try another station."}) {
                status(activity, message);
                assertEquals("Play POWER 98 EDM Club Hits", activity.findViewById(R.id.mini_toggle).getContentDescription());
            }
            assertTrue(((TextView) activity.findViewById(R.id.playback_status)).getText().toString().contains("unavailable"));
        }
    }

    @Test public void languagePickerUsesNativeNamesAndEachLocaleLocalizesScreenLabels() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            controller.get().findViewById(R.id.app_language).performClick();
            AlertDialog dialog = ShadowAlertDialog.getLatestAlertDialog();
            String[] names = {"English", "中文", "Bahasa Melayu", "தமிழ்"};
            for (int i = 0; i < names.length; i++) assertEquals(names[i], dialog.getListView().getAdapter().getItem(i));
            dialog.dismiss();
        }
        String[] tags = {"zh", "ms", "ta"};
        String[] labels = {"所有电台", "Semua stesen", "அனைத்து நிலையங்களும்"};
        for (int i = 0; i < tags.length; i++) {
            RuntimeEnvironment.getApplication().getSharedPreferences(MainActivity.PREFERENCES, 0).edit().putString("language", tags[i]).commit();
            try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
                assertEquals(labels[i], ((TextView) controller.get().findViewById(R.id.tab_all)).getText().toString());
                assertTrue(containsText((ViewGroup) list(controller.get()).getChildAt(0), "YES 933"));
            }
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp-mdpi")
    public void largeTextKeepsStationNamesAndBothMiniBarActionsReachable() {
        RuntimeEnvironment.setFontScale(1.6f);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            list(activity).findViewWithTag("play:LOVE 972").performClick();
            activity.findViewById(R.id.browse_stations).performClick();
            layout(activity, 360, 640);
            View bar = activity.findViewById(R.id.mini_player);
            assertTrue(bar.getBottom() <= 640);
            assertTrue(activity.findViewById(R.id.mini_toggle).getHeight() >= 48);
            assertTrue(activity.findViewById(R.id.open_player).getWidth() > 100);
            assertTrue(activity.findViewById(R.id.station_scroller).getHeight() - bar.getHeight() >= 200);
            for (int i = 0; i < list(activity).getChildCount(); i++) {
                LinearLayout row = (LinearLayout) list(activity).getChildAt(i);
                assertEquals(LinearLayout.VERTICAL, row.getOrientation());
                assertTextFits(row);
            }
        }
    }

    static void status(MainActivity activity, String status) {
        activity.sendBroadcast(new Intent(RadioService.BROADCAST_STATUS).setPackage(activity.getPackageName()).putExtra("status", status));
        // Allow pending frame barriers to clear before checking the status receiver's result.
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32));
    }

    static LinearLayout list(MainActivity activity) { return activity.findViewById(R.id.station_list); }
    static void showWindow(MainActivity activity) {
        // Advance animation frames explicitly; automatic vsync never idles for a repeating ring.
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        org.robolectric.shadows.ShadowChoreographer.setFrameDelay(java.time.Duration.ofMillis(16));
        // Offscreen Robolectric windows need the visibility signal normally sent by WindowManager.
        Object root = org.robolectric.util.ReflectionHelpers.callInstanceMethod(
            activity.getWindow().getDecorView(), "getViewRootImpl");
        org.robolectric.util.ReflectionHelpers.callInstanceMethod(root, "handleAppVisibility",
            org.robolectric.util.ReflectionHelpers.ClassParameter.from(boolean.class, true));
        shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofMillis(32));
    }
    static void query(MainActivity activity, String query) { ((EditText) activity.findViewById(R.id.station_search)).setText(query); }
    static void layout(MainActivity activity, int width, int height) {
        View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        // A second layout incorporates measured bottom-bar padding.
        for (int i = 0; i < 2; i++) {
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);
            shadowOf(Looper.getMainLooper()).idle();
        }
    }

    private static void assertPlayIntent(MainActivity activity, Station station) {
        Intent intent = shadowOf(activity).getNextStartedService();
        assertNotNull(station.name, intent);
        assertEquals(RadioService.ACTION_PLAY, intent.getAction());
        assertEquals(station.name, intent.getStringExtra(RadioService.EXTRA_NAME));
        assertEquals(station.url, intent.getStringExtra(RadioService.EXTRA_URL));
    }

    private static boolean containsText(ViewGroup group, String text) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView && text.contentEquals(((TextView) child).getText())) return true;
            if (child instanceof ViewGroup && containsText((ViewGroup) child, text)) return true;
        }
        return false;
    }

    private static void assertTextFits(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) {
                TextView text = (TextView) child;
                assertNotNull(text.getLayout());
                assertTrue("Text fits: " + text.getText(),
                    text.getLayout().getHeight() <= text.getHeight() - text.getCompoundPaddingTop() - text.getCompoundPaddingBottom());
            }
            if (child instanceof ViewGroup) assertTextFits((ViewGroup) child);
        }
    }
}
