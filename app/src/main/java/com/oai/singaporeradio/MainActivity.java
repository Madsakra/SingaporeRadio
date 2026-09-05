package com.oai.singaporeradio;

import android.Manifest;
import android.app.Activity;
import android.content.*;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.AbsoluteSizeSpan;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.LinkedHashMap;
import java.util.Map;

/** Presentation only: playback commands and RadioService remain unchanged. */
public class MainActivity extends Activity {
    private static final int BACKGROUND = Color.rgb(227, 229, 228);
    private static final int SURFACE = Color.rgb(240, 241, 239);
    private static final int TEXT = Color.rgb(32, 39, 37);
    private static final int MUTED = Color.rgb(76, 85, 80);
    private static final int GREEN = Color.rgb(32, 92, 73);
    private static final int RED = Color.rgb(148, 63, 69);
    private static final int BUTTON_TEXT = Color.rgb(245, 245, 239);
    private static final int BORDER = Color.rgb(185, 193, 186);
    private static final int SELECTED = Color.rgb(212, 227, 217);
    private TextView status;
    private TextView statusSymbol;
    private final Map<String, LinearLayout> stationRows = new LinkedHashMap<>();
    private boolean narrow;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (RadioService.BROADCAST_STATUS.equals(intent.getAction())) {
                showStatus(intent.getStringExtra("status"));
            }
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        buildUi();
    }

    private void buildUi() {
        narrow = getResources().getConfiguration().screenWidthDp <= 370;
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        LinearLayout root = column();
        root.setBackgroundColor(BACKGROUND);
        int side = dp(narrow ? 12 : 16);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(side + bars.left, dp(24) + bars.top, side + bars.right, dp(10) + bars.bottom);
            return windowInsets;
        });

        // The list can scroll on short screens or at large font sizes; STOP stays visible.
        ScrollView scroller = new ScrollView(this);
        scroller.setVerticalScrollBarEnabled(false);
        scroller.setClipToPadding(false);
        LinearLayout content = column();
        scroller.addView(content, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroller, new LinearLayout.LayoutParams(-1, 0, 1));

        TextView title = label(getString(R.string.radio_title), 30, TEXT, true);
        SpannableString titleText = new SpannableString(getString(R.string.radio_title));
        int radioWord = titleText.toString().indexOf("Radio");
        titleText.setSpan(new AbsoluteSizeSpan(sp(26)), radioWord, titleText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(titleText);
        content.addView(title, new LinearLayout.LayoutParams(-1, -2));
        TextView instruction = label(getString(R.string.choose_station), 18, MUTED, false);
        LinearLayout.LayoutParams instructionParams = new LinearLayout.LayoutParams(-1, -2);
        instructionParams.topMargin = dp(8);
        instructionParams.bottomMargin = dp(18);
        content.addView(instruction, instructionParams);

        LinearLayout state = new LinearLayout(this);
        state.setGravity(Gravity.CENTER_VERTICAL);
        state.setMinimumHeight(dp(56));
        statusSymbol = label("■", 22, TEXT, true);
        statusSymbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(-2, -2);
        symbolParams.rightMargin = dp(10);
        state.addView(statusSymbol, symbolParams);
        status = label("", 20, TEXT, true);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        state.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
        stateParams.bottomMargin = dp(16);
        content.addView(state, stateParams);

        stationRows.clear();
        for (int index = 0; index < StationData.ALL.size(); index++) {
            Station station = StationData.ALL.get(index);
            LinearLayout row = buildStationRow(station);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(-1, -2);
            if (index > 0) rowParams.topMargin = dp(12);
            content.addView(row, rowParams);
            stationRows.put(station.name, row);
        }

        Button stop = actionButton(getString(R.string.stop_button), 30, RED, 94, 16);
        stop.setContentDescription(getString(R.string.stop_accessibility));
        SpannableString stopText = new SpannableString(getString(R.string.stop_button));
        int chinese = stopText.toString().indexOf("停止");
        stopText.setSpan(new AbsoluteSizeSpan(sp(26)), chinese, stopText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        stop.setText(stopText);
        stop.setOnClickListener(v -> {
            Intent i = new Intent(this, RadioService.class).setAction(RadioService.ACTION_STOP);
            startService(i);
            showStatus("Stopped");
        });
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, -2);
        stopParams.topMargin = dp(24);
        root.addView(stop, stopParams);
        TextView footer = label(getString(R.string.internet_required), 16, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(16);
        root.addView(footer, footerParams);

        setContentView(root);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(root);
        showStatus(RadioService.getLatestStatus());
    }

    private LinearLayout buildStationRow(Station station) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(128));
        row.setPadding(dp(narrow ? 10 : 12), dp(16), dp(narrow ? 10 : 12), dp(16));
        row.setBackground(rounded(SURFACE, 16, BORDER));
        ImageView logo = new ImageView(this);
        int imageResource = station.name.equals("YES 933") ? R.drawable.station_yes_933
            : station.name.equals("LOVE 972") ? R.drawable.station_love_972 : R.drawable.station_capital_958;
        logo.setImageResource(imageResource);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(rounded(station.name.equals("YES 933") ? Color.rgb(40, 49, 44) : Color.rgb(246, 245, 240), 12, null));
        logo.setClipToOutline(true);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(narrow ? 54 : 64), dp(narrow ? 54 : 64));
        logoParams.rightMargin = dp(narrow ? 8 : 10);
        row.addView(logo, logoParams);

        LinearLayout identity = column();
        int numberIndex = station.name.lastIndexOf(' ') + 1;
        identity.addView(label(station.name.substring(0, numberIndex).trim(), narrow ? 15 : 16, TEXT, true));
        identity.addView(label(station.name.substring(numberIndex), 34, TEXT, true));
        TextView frequency = label(station.subtitle.split(" • ")[0], 16, MUTED, false);
        LinearLayout.LayoutParams frequencyParams = new LinearLayout.LayoutParams(-1, -2);
        frequencyParams.topMargin = dp(3);
        identity.addView(frequency, frequencyParams);
        LinearLayout.LayoutParams identityParams = new LinearLayout.LayoutParams(0, -2, 1);
        identityParams.rightMargin = dp(narrow ? 8 : 10);
        row.addView(identity, identityParams);

        Button play = actionButton(getString(R.string.play_button), narrow ? 22 : 24, GREEN, 76, 12);
        SpannableString playText = new SpannableString(getString(R.string.play_button));
        int secondLine = playText.toString().indexOf('\n') + 1;
        playText.setSpan(new AbsoluteSizeSpan(sp(20)), secondLine, playText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        play.setText(playText);
        play.setLineSpacing(dp(5), 1f);
        play.setContentDescription(getString(R.string.play_accessibility, station.name));
        play.setOnClickListener(v -> playStation(station));
        row.addView(play, new LinearLayout.LayoutParams(dp(narrow ? 100 : 112), -2));
        return row;
    }

    private void showStatus(String message) {
        if (message == null) return;
        boolean playing = message.startsWith("Playing ");
        String stationName = playing ? message.substring("Playing ".length()) : null;
        String display = message;
        if (message.equals("Stopped")) display = getString(R.string.stopped);
        else if (playing) display = getString(R.string.playing, stationName);
        else if (message.startsWith("Connecting to ")) display = getString(R.string.connecting, message.substring("Connecting to ".length()));
        else if (message.startsWith("Buffering ")) display = getString(R.string.buffering, message.substring("Buffering ".length()));
        status.setText(display);
        status.setTextColor(playing ? GREEN : TEXT);
        statusSymbol.setText(playing ? "▶" : "■");
        statusSymbol.setTextColor(playing ? GREEN : TEXT);
        for (Map.Entry<String, LinearLayout> entry : stationRows.entrySet()) {
            boolean active = entry.getKey().equals(stationName);
            entry.getValue().setBackground(rounded(active ? SELECTED : SURFACE, 16, active ? GREEN : BORDER));
        }
    }

    private Button actionButton(String text, int size, int color, int height, int radius) {
        Button button = new Button(this);
        button.setAllCaps(false);
        button.setText(text);
        button.setTextSize(size);
        button.setTextColor(BUTTON_TEXT);
        button.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        button.setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= 28) button.setFallbackLineSpacing(false);
        button.setLetterSpacing(0);
        button.setGravity(Gravity.CENTER);
        button.setPadding(dp(8), dp(10), dp(8), dp(10));
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(dp(height));
        button.setMinimumHeight(dp(height));
        button.setStateListAnimator(null);
        button.setElevation(0);
        button.setBackgroundTintList(null);
        button.setBackground(new RippleDrawable(ColorStateList.valueOf(Color.argb(45, 255, 255, 255)), rounded(color, radius, null), null));
        return button;
    }

    private GradientDrawable rounded(int color, int radius, Integer stroke) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (stroke != null) shape.setStroke(dp(2), stroke);
        return shape;
    }

    private LinearLayout column() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        return layout;
    }

    private TextView label(String text, int size, int color, boolean bold) {
        TextView label = new TextView(this);
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        label.setIncludeFontPadding(false);
        if (Build.VERSION.SDK_INT >= 28) label.setFallbackLineSpacing(false);
        return label;
    }

    private void playStation(Station station) {
        showStatus("Connecting to " + station.name + "…");
        Intent i = new Intent(this, RadioService.class)
            .setAction(RadioService.ACTION_PLAY)
            .putExtra(RadioService.EXTRA_URL, station.url)
            .putExtra(RadioService.EXTRA_NAME, station.name);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    @Override protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(RadioService.BROADCAST_STATUS);
        androidx.core.content.ContextCompat.registerReceiver(this, receiver, filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED);
        showStatus(RadioService.getLatestStatus());
    }

    @Override protected void onStop() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        super.onStop();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private int sp(int n) { return (int)(n * getResources().getDisplayMetrics().scaledDensity + 0.5f); }
}
