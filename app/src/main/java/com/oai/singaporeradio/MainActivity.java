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

/** Accessible station browser backed by the shared station catalog. */
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
    private boolean stacked;
    private LinearLayout stationList;
    private ScrollView scroller;
    private TextView stationCount;
    private Spinner languageFilter;
    private int filterIndex;
    private static final String STATE_FILTER = "station_filter";
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
        filterIndex = savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_FILTER, 0);
        buildUi();
    }

    private void buildUi() {
        narrow = getResources().getConfiguration().screenWidthDp <= 370;
        stacked = narrow || getResources().getConfiguration().fontScale >= 1.3f;
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        LinearLayout root = column();
        root.setBackgroundColor(BACKGROUND);
        int side = dp(narrow ? 12 : 16);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(side + bars.left, dp(12) + bars.top, side + bars.right, dp(10) + bars.bottom);
            return windowInsets;
        });

        // Heading, filters and stations share the scroll area so short screens keep room for playback controls.
        scroller = new ScrollView(this);
        scroller.setId(R.id.station_scroller);
        scroller.setVerticalScrollBarEnabled(true);
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
        instructionParams.bottomMargin = dp(8);
        content.addView(instruction, instructionParams);

        LinearLayout state = new LinearLayout(this);
        state.setGravity(Gravity.CENTER_VERTICAL);
        state.setMinimumHeight(dp(56));
        statusSymbol = label("■", 22, TEXT, true);
        statusSymbol.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams symbolParams = new LinearLayout.LayoutParams(-2, -2);
        symbolParams.rightMargin = dp(10);
        state.addView(statusSymbol, symbolParams);
        status = label("", 18, TEXT, true);
        status.setId(R.id.playback_status);
        status.setMaxLines(3);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        state.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams stateParams = new LinearLayout.LayoutParams(-1, -2);
        stateParams.bottomMargin = dp(8);


        languageFilter = new Spinner(this);
        languageFilter.setId(R.id.station_filter);
        languageFilter.setContentDescription(getString(R.string.filter_language));
        languageFilter.setMinimumHeight(dp(48));
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
            R.array.station_filters, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        languageFilter.setAdapter(filterAdapter);
        if (filterIndex < 0 || filterIndex >= filterAdapter.getCount()) filterIndex = 0;
        languageFilter.setSelection(filterIndex);
        content.addView(languageFilter, new LinearLayout.LayoutParams(-1, -2));

        stationCount = label("", 16, MUTED, false);
        stationCount.setId(R.id.station_count);
        content.addView(stationCount, new LinearLayout.LayoutParams(-1, -2));

        // Playback status and STOP sit below the scrolling content.
        stationList = column();
        stationList.setId(R.id.station_list);
        stationList.setPadding(0, dp(8), 0, dp(8));
        content.addView(stationList, new LinearLayout.LayoutParams(-1, -2));
        renderStations();
        languageFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (filterIndex == position) return;
                filterIndex = position;
                renderStations();
                scroller.scrollTo(0, 0);
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        });

        root.addView(state, stateParams);

        Button stop = actionButton(getString(R.string.stop_button), 26, RED, 72, 16);
        stop.setId(R.id.stop_button);
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
        stopParams.topMargin = dp(8);
        root.addView(stop, stopParams);
        TextView footer = label(getString(R.string.internet_required), 16, MUTED, false);
        footer.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(-1, -2);
        footerParams.topMargin = dp(8);
        root.addView(footer, footerParams);

        setContentView(root);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(root);
        showStatus(RadioService.getLatestStatus());
    }

    private void renderStations() {
        Station.Language language = filterIndex == 0 ? null : Station.Language.values()[filterIndex - 1];
        java.util.List<Station> stations = StationData.forLanguage(language);
        stationList.removeAllViews();
        stationRows.clear();
        for (Station station : stations) {
            LinearLayout row = buildStationRow(station);
            row.setTag(station.name);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
            if (stationList.getChildCount() > 0) params.topMargin = dp(12);
            stationList.addView(row, params);
            stationRows.put(station.name, row);
        }
        stationCount.setText(getResources().getQuantityString(R.plurals.station_count, stations.size(), stations.size()));
        showStatus(RadioService.getLatestStatus());
    }

    private LinearLayout buildStationRow(Station station) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(stacked ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setMinimumHeight(dp(128));
        row.setPadding(dp(12), dp(16), dp(12), dp(16));
        row.setBackground(rounded(SURFACE, 16, BORDER));

        LinearLayout identity = new LinearLayout(this);
        identity.setGravity(Gravity.CENTER_VERTICAL);
        ImageView logo = new ImageView(this);
        logo.setImageResource(stationLogo(station));
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logo.setBackground(rounded(station.name.equals("YES 933") ? Color.rgb(40, 49, 44) : Color.rgb(246, 245, 240), 12, null));
        logo.setClipToOutline(true);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        LinearLayout.LayoutParams logoParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        logoParams.rightMargin = dp(10);
        identity.addView(logo, logoParams);

        LinearLayout text = column();
        // Whole names wrap naturally, including numeric names and digital channel names.
        text.addView(label(station.name, 24, TEXT, true), new LinearLayout.LayoutParams(-1, -2));
        TextView subtitle = label(station.subtitle, 16, MUTED, false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(-1, -2);
        subtitleParams.topMargin = dp(4);
        text.addView(subtitle, subtitleParams);
        identity.addView(text, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams identityParams = stacked
            ? new LinearLayout.LayoutParams(-1, -2) : new LinearLayout.LayoutParams(0, -2, 1);
        if (!stacked) identityParams.rightMargin = dp(10);
        row.addView(identity, identityParams);

        Button play = actionButton(getString(R.string.play_button), 24, GREEN, 76, 12);
        SpannableString playText = new SpannableString(getString(R.string.play_button));
        int secondLine = playText.toString().indexOf('\n') + 1;
        playText.setSpan(new AbsoluteSizeSpan(sp(20)), secondLine, playText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        play.setText(playText);
        play.setLineSpacing(dp(5), 1f);
        play.setContentDescription(getString(R.string.play_accessibility, station.name));
        play.setOnClickListener(v -> playStation(station));
        LinearLayout.LayoutParams playParams = new LinearLayout.LayoutParams(stacked ? -1 : dp(112), -2);
        if (stacked) playParams.topMargin = dp(12);
        row.addView(play, playParams);
        return row;
    }

    private int stationLogo(Station station) {
        switch (station.name) {
            case "YES 933": return R.drawable.station_yes_933;
            case "LOVE 972": return R.drawable.station_love_972;
            case "CAPITAL 958": return R.drawable.station_capital_958;
            case "96.3好FM": return R.drawable.station_hao_963;
            case "UFM100.3": return R.drawable.station_ufm_1003;
            case "MONEY FM 89.3": return R.drawable.station_money_893;
            case "GOLD 905": return R.drawable.station_gold_905;
            case "ONE FM 91.3": return R.drawable.station_one_913;
            case "Kiss92 FM": return R.drawable.station_kiss_92;
            case "Symphony 924": return R.drawable.station_symphony_924;
            case "CNA938": return R.drawable.station_cna_938;
            case "CLASS 95": return R.drawable.station_class_95;
            case "987": return R.drawable.station_987;
            case "RIA 897": return R.drawable.station_ria_897;
            case "WARNA 942": return R.drawable.station_warna_942;
            case "OLI 968": return R.drawable.station_oli_968;
            case "BBC World Service": return R.drawable.station_bbc_world_service;
            case "indiego": return R.drawable.station_indiego;
            case "88.3JIA": return R.drawable.station_jia_883;
            case "POWER 98": return R.drawable.station_power_98;
            case "88.3JIA Trending Hits": return R.drawable.station_jia_trending;
            case "88.3JIA Cantopop": return R.drawable.station_jia_cantopop;
            case "88.3JIA K-Pop": return R.drawable.station_jia_kpop;
            case "POWER 98 Mixtape": return R.drawable.station_power_98_mixtape;
            case "POWER 98 EDM Club Hits": return R.drawable.station_power_98_edm;
            case "POWER 98 EMERGENC-E": return R.drawable.station_power_98_emergenc_e;
            default: return R.drawable.ic_radio;
        }
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
        if (Build.VERSION.SDK_INT >= 28) label.setFallbackLineSpacing(true);
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

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt(STATE_FILTER, filterIndex);
        super.onSaveInstanceState(state);
    }

    @Override protected void onStop() {
        try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        super.onStop();
    }

    private int dp(int n) { return (int)(n * getResources().getDisplayMetrics().density + 0.5f); }
    private int sp(int n) { return (int)(n * getResources().getDisplayMetrics().scaledDensity + 0.5f); }
}
