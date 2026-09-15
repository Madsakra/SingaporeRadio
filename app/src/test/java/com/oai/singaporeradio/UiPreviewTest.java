package com.oai.singaporeradio;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
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
import static org.robolectric.Shadows.shadowOf;

/** Renders Android views for manual visual review in build/reports/ui/. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 36, qualifiers = "w411dp-h891dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class UiPreviewTest {
    @Test public void renderCatalogAndFilters() throws Exception {
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            MainActivity activity = controller.get();
            Spinner filter = activity.findViewById(R.id.station_filter);
            capture(activity, "all-stations", 411, 891);
            filter.setSelection(3);
            shadowOf(android.os.Looper.getMainLooper()).idle();
            capture(activity, "malay", 411, 891);
            filter.setSelection(6);
            shadowOf(android.os.Looper.getMainLooper()).idle();
            capture(activity, "korean", 411, 891);
        }
    }

    @Test @Config(qualifiers = "w360dp-h640dp-mdpi")
    public void renderLargeTextOnSmallPhone() throws Exception {
        RuntimeEnvironment.setFontScale(1.6f);
        try (ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class).setup()) {
            capture(controller.get(), "large-text", 360, 640);
        }
    }

    private static void capture(MainActivity activity, String name, int width, int height) throws Exception {
        View root = ((ViewGroup) activity.findViewById(android.R.id.content)).getChildAt(0);
        root.measure(View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, width, height);
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        root.draw(new Canvas(bitmap));
        assertTrue("Rendered view contains foreground content", bitmap.getPixel(width / 2, height - 80) != bitmap.getPixel(0, 0));
        File directory = new File("build/reports/ui");
        assertTrue(directory.isDirectory() || directory.mkdirs());
        try (FileOutputStream output = new FileOutputStream(new File(directory, name + ".png"))) {
            assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output));
        }
        bitmap.recycle();
    }
}
