package com.oai.singaporeradio;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = {24, 36}, qualifiers = "w411dp-h891dp")
public class MainActivityTest {
    @Before public void grantInstallTimeSignaturePermission() {
        // Android grants the app's own merged signature permission at installation.
        // Robolectric API 24 needs that install-time grant supplied explicitly.
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(
            "com.oai.singaporeradio.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION");
    }

    @Test public void everyStationRendersItsFullNameAndStartsItsOwnStream() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            LinearLayout list = activity.findViewById(R.id.station_list);
            assertEquals(StationData.ALL.size(), list.getChildCount());
            assertNull(shadowOf(activity).getNextStartedService());
            for (int i = 0; i < StationData.ALL.size(); i++) {
                Station station = StationData.ALL.get(i);
                ViewGroup row = (ViewGroup) list.getChildAt(i);
                List<TextView> labels = labels(row);
                assertTrue(station.name, labels.stream().anyMatch(v -> station.name.contentEquals(v.getText())));
                assertTrue(station.name, labels.stream().anyMatch(v -> station.subtitle.contentEquals(v.getText())));
                Button play = (Button) row.getChildAt(row.getChildCount() - 1);
                assertEquals("Play " + station.name, play.getContentDescription());
                play.performClick();
                Intent intent = shadowOf(activity).getNextStartedService();
                assertNotNull(station.name, intent);
                assertEquals(RadioService.ACTION_PLAY, intent.getAction());
                assertEquals(station.url, intent.getStringExtra(RadioService.EXTRA_URL));
                assertEquals(station.name, intent.getStringExtra(RadioService.EXTRA_NAME));
            }
            activity.findViewById(R.id.stop_button).performClick();
            assertEquals(RadioService.ACTION_STOP, shadowOf(activity).getNextStartedService().getAction());
        }
    }

    @Test public void filteringChangesRowsWithoutStartingOrStoppingPlaybackAndSurvivesRecreation() {
        Bundle state = new Bundle();
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            Spinner filter = activity.findViewById(R.id.station_filter);
            assertEquals(Station.Language.values().length + 1, filter.getCount());
            for (int i = 1; i < filter.getCount(); i++) {
                filter.setSelection(i);
                shadowOf(android.os.Looper.getMainLooper()).idle();
                LinearLayout list = activity.findViewById(R.id.station_list);
                List<Station> expected = StationData.forLanguage(Station.Language.values()[i - 1]);
                assertEquals(expected.size(), list.getChildCount());
                for (int j = 0; j < expected.size(); j++) assertEquals(expected.get(j).name, list.getChildAt(j).getTag());
            }
            assertNull(shadowOf(activity).getNextStartedService());
            controller.saveInstanceState(state);
        }
        try (ActivityController<MainActivity> restored = Robolectric.buildActivity(MainActivity.class).setup(state)) {
            Spinner filter = restored.get().findViewById(R.id.station_filter);
            assertEquals(Station.Language.values().length, filter.getSelectedItemPosition());
            LinearLayout list = restored.get().findViewById(R.id.station_list);
            assertEquals(1, list.getChildCount());
            assertEquals("88.3JIA K-Pop", list.getChildAt(0).getTag());
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp")
    public void narrowScreenAndLargeTextKeepControlsVisibleAndRowsScrollable() {
        Configuration config = new Configuration(RuntimeEnvironment.getApplication().getResources().getConfiguration());
        config.fontScale = 1.6f;
        RuntimeEnvironment.getApplication().getResources().updateConfiguration(config, null);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            float density = activity.getResources().getDisplayMetrics().density;
            int width = (int) (360 * density), height = (int) (640 * density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);
            View stop = activity.findViewById(R.id.stop_button);
            View scroller = activity.findViewById(R.id.station_scroller);
            assertTrue("STOP fits on screen", stop.getBottom() <= height);
            assertTrue("STOP remains tappable", stop.getHeight() >= 48 * density);
            assertTrue("Station list has visible space", scroller.getHeight() > 80 * density);
            assertNotSame(scroller, stop.getParent());
            assertNotSame(scroller, activity.findViewById(R.id.playback_status).getParent());
            LinearLayout list = activity.findViewById(R.id.station_list);
            assertTrue(list.getHeight() > scroller.getHeight());
            for (int i = 0; i < list.getChildCount(); i++) {
                LinearLayout row = (LinearLayout) list.getChildAt(i);
                assertEquals(LinearLayout.VERTICAL, row.getOrientation());
                for (TextView label : labels(row)) {
                    assertTrue(label.getText().toString(), label.getWidth() > 0);
                    assertNotNull(label.getLayout());
                    int last = label.getLayout().getLineCount() - 1;
                    assertEquals("No ellipsized station labels", 0, label.getLayout().getEllipsisCount(last));
                    assertTrue("Text fits its view: " + label.getText(),
                        label.getLayout().getHeight() <= label.getHeight() - label.getCompoundPaddingTop() - label.getCompoundPaddingBottom());
                }
            }
        }
    }

    @Test @Config(qualifiers = "w640dp-h360dp-land")
    public void landscapeLeavesRoomToChooseAnotherStationAfterAnError() {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            activity.sendBroadcast(new Intent(RadioService.BROADCAST_STATUS).setPackage(activity.getPackageName())
                .putExtra("status", "Unable to play POWER 98 EDM Club Hits. The stream may be unavailable or region-restricted. Try another station."));
            shadowOf(android.os.Looper.getMainLooper()).idle();
            View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
            float density = activity.getResources().getDisplayMetrics().density;
            int width = (int) (640 * density), height = (int) (360 * density);
            root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
            root.layout(0, 0, width, height);
            assertTrue(activity.findViewById(R.id.station_scroller).getHeight() >= 80 * density);
            assertTrue(activity.findViewById(R.id.stop_button).getBottom() <= height);
        }
    }

    private static List<TextView> labels(ViewGroup group) {
        List<TextView> result = new ArrayList<>();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof TextView) result.add((TextView) child);
            if (child instanceof ViewGroup) result.addAll(labels((ViewGroup) child));
        }
        return result;
    }
}
