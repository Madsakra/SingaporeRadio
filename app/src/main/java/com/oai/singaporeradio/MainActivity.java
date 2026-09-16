package com.oai.singaporeradio;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import static com.oai.singaporeradio.RadioUi.*;

/** Station browsing and player presentation, using the existing service commands unchanged. */
public class MainActivity extends Activity {
    static final String PREFERENCES = "radio_ui";
    private static final String[] LOCALE_TAGS = {"en", "zh", "ms", "ta"};
    private static final String[] LOCALE_NAMES = {"English", "中文", "Bahasa Melayu", "தமிழ்"};
    private RadioUi ui;
    private SharedPreferences preferences;
    private Set<String> favourites;
    private final List<Station> queue = new ArrayList<>();
    private final Map<String, RowViews> rows = new LinkedHashMap<>();
    private List<Station> visibleStations = new ArrayList<>();
    private Station selected;
    private PlaybackPresentation playback = PlaybackPresentation.from("Stopped");
    private int filterIndex;
    private boolean favouritesTab, queueFavourites, playerOpen, stacked, backRegistered, resumed;
    private String query = "";
    private FrameLayout browser;
    private ScrollView scroller, focus;
    private LinearLayout stationList, emptyState, miniBar;
    private TextView stationCount, filterValue, miniName, miniAction;
    private TextView playerName, playerSubtitle, playerStatus, toggleLabel, queueLabel;
    private Button allTab, savedTab, playerFavourite;
    private EditText search;
    private ImageView miniLogo, playerLogo;
    private ImageButton miniToggle, playerToggle, previous, next;
    private LoadingRingView miniLoading, playerLoading;
    private PlayerTransition playerTransition;
    private View openPlayer;
    private android.window.OnBackInvokedCallback backCallback;

    private static final class RowViews {
        final LinearLayout row;
        final Button play;
        RowViews(LinearLayout row, Button play) { this.row = row; this.play = play; }
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (RadioService.BROADCAST_STATUS.equals(intent.getAction()))
                showStatus(intent.getStringExtra("status"));
        }
    };

    @Override protected void attachBaseContext(Context base) {
        String language = base.getSharedPreferences(PREFERENCES, MODE_PRIVATE).getString("language", "en");
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(Locale.forLanguageTag(language));
        super.attachBaseContext(base.createConfigurationContext(configuration));
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        ui = new RadioUi(this);
        preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE);
        favourites = new HashSet<>(preferences.getStringSet("favourites", new HashSet<>()));
        selected = StationPresentation.named(preferences.getString("selected", ""));
        restoreQueue();
        if (state != null) {
            filterIndex = state.getInt("filter", 0);
            favouritesTab = state.getBoolean("favourites_tab");
            query = state.getString("query", "");
            playerOpen = state.getBoolean("player_open");
        }
        if (filterIndex < 0 || filterIndex > Station.Language.values().length) filterIndex = 0;
        stacked = getResources().getConfiguration().screenWidthDp <= 370
            || getResources().getConfiguration().fontScale >= 1.3f
            || getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ta");
        buildUi();
        if (state != null) {
            int browserScroll = state.getInt("browser_scroll");
            int playerScroll = state.getInt("player_scroll");
            scroller.post(() -> scroller.scrollTo(0, browserScroll));
            focus.post(() -> focus.scrollTo(0, playerScroll));
        }
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
    }

    private void buildUi() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        if (Build.VERSION.SDK_INT >= 29) getWindow().setNavigationBarContrastEnforced(false);
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BACKGROUND);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            Insets keyboard = insets.getInsets(WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, Math.max(bars.bottom, keyboard.bottom));
            return insets;
        });
        browser = new FrameLayout(this);
        root.addView(browser, new FrameLayout.LayoutParams(-1, -1));
        buildBrowser();
        buildMiniPlayer();
        buildFocusPlayer();
        root.addView(focus, new FrameLayout.LayoutParams(-1, -1));
        View transitionShield = new View(this);
        transitionShield.setId(R.id.player_transition_shield);
        transitionShield.setClickable(true);
        transitionShield.setFocusable(false);
        transitionShield.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        transitionShield.setVisibility(View.GONE);
        root.addView(transitionShield, new FrameLayout.LayoutParams(-1, -1));
        playerTransition = new PlayerTransition(browser, focus, transitionShield);
        setContentView(root);
        WindowInsetsControllerCompat controller = WindowCompat.getInsetsController(getWindow(), root);
        controller.setAppearanceLightStatusBars(true);
        controller.setAppearanceLightNavigationBars(true);
        ViewCompat.requestApplyInsets(root);
        renderStations();
        showStatus(RadioService.getLatestStatus());
        setPlayerOpen(playerOpen && selected != null);
    }

    private void buildBrowser() {
        scroller = new ScrollView(this);
        scroller.setId(R.id.station_scroller);
        scroller.setFillViewport(true);
        scroller.setClipToPadding(false);
        scroller.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        scroller.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16));
        LinearLayout content = ui.column();
        scroller.addView(content, new ScrollView.LayoutParams(-1, -2));
        browser.addView(scroller, new FrameLayout.LayoutParams(-1, -1));
        content.addView(header(false), lp(-1, -2, 0, 16));

        LinearLayout filter = ui.row();
        filter.setId(R.id.station_filter);
        filter.setPadding(ui.dp(14), ui.dp(10), ui.dp(14), ui.dp(10));
        filter.setMinimumHeight(ui.dp(64));
        ui.clickable(filter, BACKGROUND, 8, BORDER);
        LinearLayout filterText = ui.column();
        filterText.addView(ui.text(getString(R.string.filter_language), 15, MUTED, false));
        filterValue = ui.text("", 19, TEXT, true);
        filterText.addView(filterValue, lp(-1, -2, 3, 0));
        filter.addView(filterText, new LinearLayout.LayoutParams(0, -2, 1));
        filter.addView(ui.image(R.drawable.ic_ui_chevron_down, 24, TEXT), lp(ui.dp(24), ui.dp(24), 0, 0));
        filter.setOnClickListener(v -> showStationLanguages());
        content.addView(filter, lp(-1, -2, 0, 14));

        LinearLayout searchBox = ui.row();
        searchBox.setBackground(ui.surface(BACKGROUND, 8, BORDER));
        searchBox.setPadding(ui.dp(12), 0, ui.dp(2), 0);
        searchBox.addView(ui.image(R.drawable.ic_ui_search, 24, TEXT), lp(ui.dp(24), ui.dp(24), 0, 0));
        search = new EditText(this);
        search.setId(R.id.station_search);
        search.setSingleLine(true);
        search.setTextSize(18);
        search.setTextColor(TEXT);
        search.setHintTextColor(MUTED);
        search.setHint(R.string.search_hint);
        search.setContentDescription(getString(R.string.search_hint));
        search.setBackgroundColor(Color.TRANSPARENT);
        search.setPadding(ui.dp(12), ui.dp(12), 0, ui.dp(12));
        search.setMinimumHeight(ui.dp(56));
        search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);
        search.setText(query);
        search.setSaveEnabled(false); // Query is restored explicitly, independently of the view tree.
        searchBox.addView(search, new LinearLayout.LayoutParams(0, -2, 1));
        ImageButton clear = ui.iconButton(R.drawable.ic_ui_close, MUTED, Color.TRANSPARENT, 20, 48, getString(R.string.clear_search));
        clear.setId(R.id.clear_search);
        clear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
        clear.setOnClickListener(v -> search.setText(""));
        searchBox.addView(clear, lp(ui.dp(48), ui.dp(48), 0, 0));
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                query = s.toString();
                clear.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                renderStations();
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        search.setOnEditorActionListener((view, action, event) -> { hideKeyboard(); return true; });
        content.addView(searchBox, lp(-1, -2, 0, 14));
        LinearLayout tabs = ui.row();
        allTab = tab(getString(R.string.all_stations), R.id.tab_all, false);
        savedTab = tab(getString(R.string.favourites), R.id.tab_favourites, true);
        tabs.addView(allTab, new LinearLayout.LayoutParams(0, -1, 1));
        tabs.addView(savedTab, new LinearLayout.LayoutParams(0, -1, 1));
        content.addView(tabs, lp(-1, -2, 0, 16));
        stationCount = ui.text("", 16, MUTED, false);
        stationCount.setId(R.id.station_count);
        content.addView(stationCount, lp(-1, -2, 0, 12));
        stationList = ui.column();
        stationList.setId(R.id.station_list);
        content.addView(stationList, lp(-1, -2, 0, 0));
        emptyState = ui.column();
        content.addView(emptyState, lp(-1, -2, 12, 0));
    }

    private View header(boolean player) {
        boolean wrap = stacked || getResources().getConfiguration().screenWidthDp < 390
            || getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ta");
        LinearLayout header = wrap ? ui.column() : ui.row();
        View heading;
        if (player) {
            Button browse = ui.button(getString(R.string.browse_stations), TEXT, Color.TRANSPARENT);
            browse.setId(R.id.browse_stations);
            browse.setPadding(0, ui.dp(8), ui.dp(8), ui.dp(8));
            browse.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            browse.setCompoundDrawables(ui.icon(R.drawable.ic_ui_chevron_down, TEXT, 24), null, null, null);
            browse.setCompoundDrawablePadding(ui.dp(8));
            browse.setOnClickListener(v -> setPlayerOpen(false));
            heading = browse;
        } else {
            heading = ui.text(getString(R.string.radio_title), 24, TEXT, true);
        }
        header.addView(heading, wrap ? lp(-1, -2, 0, 8) : new LinearLayout.LayoutParams(0, -2, 1));
        Button language = ui.button(currentLanguageName(), TEXT, BACKGROUND);
        // Separate IDs are unnecessary for the second language button; both expose the same action.
        if (!player) language.setId(R.id.app_language);
        language.setTextSize(16);
        language.setTypeface(android.graphics.Typeface.DEFAULT);
        language.setCompoundDrawables(ui.icon(R.drawable.ic_ui_globe, TEXT, 22), null, null, null);
        language.setCompoundDrawablePadding(ui.dp(6));
        language.setContentDescription(getString(R.string.app_language) + ": " + currentLanguageName());
        ui.clickable(language, BACKGROUND, 8, BORDER);
        language.setOnClickListener(v -> showAppLanguages());
        LinearLayout.LayoutParams languageParams = lp(-2, -2, 0, 0);
        if (wrap) languageParams.gravity = Gravity.END;
        else languageParams.leftMargin = ui.dp(8);
        header.addView(language, languageParams);
        return header;
    }

    private Button tab(String text, int id, boolean saved) {
        Button button = ui.button(text, TEXT, Color.TRANSPARENT);
        button.setId(id);
        button.setTextSize(18);
        button.setPadding(ui.dp(6), ui.dp(12), ui.dp(6), ui.dp(12));
        if (getResources().getConfiguration().getLocales().get(0).getLanguage().equals("ta"))
            button.setTextSize(16);
        button.setOnClickListener(v -> {
            favouritesTab = saved;
            renderStations();
        });
        return button;
    }

    private void styleTab(Button button, boolean active) {
        button.setTextColor(active ? GREEN : TEXT);
        button.setSelected(active);
        // A bottom stroke keeps the approved underlined-tab treatment.
        android.graphics.drawable.LayerDrawable background = new android.graphics.drawable.LayerDrawable(
            new android.graphics.drawable.Drawable[]{ui.surface(active ? GREEN : BORDER, 0, null),
                ui.surface(BACKGROUND, 0, null)});
        background.setLayerInset(1, 0, 0, 0, ui.dp(active ? 3 : 1));
        button.setBackground(new android.graphics.drawable.RippleDrawable(
            android.content.res.ColorStateList.valueOf(0x2207583F), background, null));
    }

    private void renderStations() {
        if (stationList == null) return;
        filterValue.setText(getString(filterIndex == 0 ? R.string.all_languages
            : StationPresentation.LANGUAGE_LABELS[filterIndex - 1]));
        updateFilterDescription();
        styleTab(allTab, !favouritesTab);
        styleTab(savedTab, favouritesTab);
        visibleStations = new ArrayList<>();
        for (Station station : StationData.forLanguage(filterIndex == 0 ? null : Station.Language.values()[filterIndex - 1])) {
            if ((!favouritesTab || favourites.contains(station.name)) && StationPresentation.matches(station, query))
                visibleStations.add(station);
        }
        stationList.removeAllViews();
        rows.clear();
        for (Station station : visibleStations) stationList.addView(stationRow(station), lp(-1, -2, 0, 8));
        stationCount.setText(getResources().getQuantityString(favouritesTab ? R.plurals.saved_count : R.plurals.station_count,
            visibleStations.size(), visibleStations.size()));
        emptyState.removeAllViews();
        if (visibleStations.isEmpty()) {
            boolean noneSaved = favouritesTab && favourites.isEmpty();
            emptyState.addView(ui.text(getString(noneSaved ? R.string.no_favourites : R.string.no_results), 22, TEXT, true));
            emptyState.addView(ui.text(getString(noneSaved ? R.string.favourites_hint : R.string.no_results_hint),
                18, MUTED, false), lp(-1, -2, 12, 16));
            Button reset = ui.button(getString(R.string.show_all), Color.WHITE, GREEN);
            reset.setOnClickListener(v -> { favouritesTab = false; filterIndex = 0; search.setText(""); renderStations(); });
            emptyState.addView(reset, lp(-1, -2, 0, 0));
        }
        updatePlaybackViews();
    }

    private void updateFilterDescription() {
        // The control is built before setContentView; access its parent directly during initial rendering.
        ((View) filterValue.getParent().getParent()).setContentDescription(
            getString(R.string.filter_language) + ": " + filterValue.getText());
    }

    private View stationRow(Station station) {
        LinearLayout row = stacked ? ui.column() : ui.row();
        row.setTag(station.name);
        row.setMinimumHeight(ui.dp(80));
        row.setPadding(ui.dp(2), ui.dp(8), ui.dp(2), ui.dp(8));
        LinearLayout identity = ui.row();
        identity.addView(ui.logo(station, 8), lp(ui.dp(56), ui.dp(56), 0, 0));
        LinearLayout names = ui.column();
        names.addView(ui.text(station.name, 20, TEXT, true));
        names.addView(ui.text(StationPresentation.subtitle(this, station), 15, MUTED, false), lp(-1, -2, 4, 0));
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, -2, 1);
        nameParams.leftMargin = ui.dp(12);
        identity.addView(names, nameParams);
        row.addView(identity, stacked ? lp(-1, -2, 0, 8) : new LinearLayout.LayoutParams(0, -2, 1));

        LinearLayout actions = ui.row();
        actions.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        boolean saved = favourites.contains(station.name);
        ImageButton heart = ui.iconButton(saved ? R.drawable.ic_ui_heart : R.drawable.ic_ui_heart_outline,
            GREEN, Color.TRANSPARENT, 28, 48, getString(saved ? R.string.remove_station : R.string.save_station, station.name));
        heart.setTag("favourite:" + station.name);
        setCheckedAccessibility(heart, saved);
        heart.setOnClickListener(v -> toggleFavourite(station));
        actions.addView(heart, lp(ui.dp(48), ui.dp(52), 0, 0));
        Button play = ui.button(getString(R.string.play_button), Color.WHITE, GREEN);
        play.setTag("play:" + station.name);
        play.setCompoundDrawablePadding(ui.dp(4));
        play.setOnClickListener(v -> {
            if (selected == station && playback.active) setPlayerOpen(true);
            else { captureQueue(); playStation(station, true); }
        });
        LinearLayout.LayoutParams playParams = lp(stacked ? -2 : ui.dp(92), -2, 0, 0);
        playParams.leftMargin = ui.dp(4);
        actions.addView(play, playParams);
        row.addView(actions, lp(stacked ? -1 : -2, -2, 0, 0));
        rows.put(station.name, new RowViews(row, play));
        return row;
    }

    private void buildMiniPlayer() {
        miniBar = ui.row();
        miniBar.setId(R.id.mini_player);
        miniBar.setPadding(ui.dp(10), ui.dp(10), ui.dp(10), ui.dp(10));
        miniBar.setMinimumHeight(ui.dp(80));
        miniBar.setBackground(ui.surface(BAR, 8, 0xFF9FBEAC));
        miniBar.setElevation(ui.dp(5));
        FrameLayout.LayoutParams barParams = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        barParams.setMargins(ui.dp(16), ui.dp(16), ui.dp(16), ui.dp(16));
        browser.addView(miniBar, barParams);
        LinearLayout open = ui.row();
        open.setId(R.id.open_player);
        openPlayer = open;
        open.setMinimumHeight(ui.dp(56));
        ui.clickable(open, Color.TRANSPARENT, 6, null);
        open.setOnClickListener(v -> setPlayerOpen(true));
        miniLogo = ui.logo(selected, 6);
        open.addView(miniLogo, lp(ui.dp(48), ui.dp(48), 0, 0));
        LinearLayout identity = ui.column();
        miniName = ui.text("", 18, TEXT, true);
        miniAction = ui.text(getString(R.string.open_player), 15, GREEN, false);
        identity.addView(miniName);
        identity.addView(miniAction, lp(-1, -2, 3, 0));
        LinearLayout.LayoutParams identityParams = new LinearLayout.LayoutParams(0, -2, 1);
        identityParams.leftMargin = ui.dp(10);
        identityParams.rightMargin = ui.dp(4);
        open.addView(identity, identityParams);
        // The final design uses a small chevron; the entire identity area is the touch target.
        open.addView(ui.image(R.drawable.ic_ui_chevron_up, 24, GREEN), lp(ui.dp(24), ui.dp(24), 0, 0));
        hideDescendantsFromAccessibility(open);
        miniBar.addView(open, new LinearLayout.LayoutParams(0, -2, 1));
        View divider = new View(this);
        divider.setBackgroundColor(0xFFAAC7B4);
        LinearLayout.LayoutParams dividerParams = lp(ui.dp(1), ui.dp(40), 0, 0);
        dividerParams.setMargins(ui.dp(10), 0, ui.dp(10), 0);
        miniBar.addView(divider, dividerParams);
        miniToggle = ui.iconButton(R.drawable.ic_ui_play, Color.WHITE, GREEN, 40, 56, "");
        miniToggle.setId(R.id.mini_toggle);
        miniToggle.setOnClickListener(v -> togglePlayback());
        FrameLayout shortcut = new FrameLayout(this);
        shortcut.addView(miniToggle, new FrameLayout.LayoutParams(-1, -1));
        miniLoading = new LoadingRingView(this, Color.WHITE, 0x47FFFFFF, 2, 5);
        miniLoading.setId(R.id.mini_loading);
        shortcut.addView(miniLoading, new FrameLayout.LayoutParams(-1, -1));
        miniBar.addView(shortcut, lp(ui.dp(56), ui.dp(56), 0, 0));
        miniBar.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> updateBrowserPadding());
    }

    private void updateBrowserPadding() {
        int bottom = miniBar.getVisibility() == View.VISIBLE ? miniBar.getHeight() + ui.dp(32) : ui.dp(16);
        if (scroller.getPaddingBottom() != bottom)
            scroller.setPadding(ui.dp(16), ui.dp(16), ui.dp(16), bottom);
    }

    private void buildFocusPlayer() {
        focus = new ScrollView(this);
        focus.setId(R.id.focus_player);
        ViewCompat.setAccessibilityPaneTitle(focus, getString(R.string.player));
        focus.setBackgroundColor(BACKGROUND);
        focus.setFillViewport(true);
        focus.setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(24));
        LinearLayout content = ui.column();
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        focus.addView(content, new ScrollView.LayoutParams(-1, -2));
        content.addView(header(true), lp(-1, -2, 0, 20));
        content.addView(ui.text(getString(R.string.player), 18, MUTED, false), lp(-1, -2, 0, 28));
        playerLogo = ui.logo(selected, 36);
        int logoSize = Math.min(228, getResources().getConfiguration().screenWidthDp - 80);
        FrameLayout artwork = new FrameLayout(this);
        // Keep the same outer footprint; leave room around the artwork for the quiet ring.
        int artSize = ui.dp(Math.round(logoSize * 0.7f));
        artwork.addView(playerLogo, new FrameLayout.LayoutParams(artSize, artSize, Gravity.CENTER));
        playerLoading = new LoadingRingView(this, GREEN, 0xFFE4EEE6, 3, 10);
        playerLoading.setId(R.id.player_loading);
        artwork.addView(playerLoading, new FrameLayout.LayoutParams(-1, -1));
        content.addView(artwork, lp(ui.dp(logoSize), ui.dp(logoSize), 0, 24));
        playerName = centred("", 34, TEXT, true);
        content.addView(playerName, lp(-1, -2, 0, 10));
        playerSubtitle = centred("", 20, MUTED, false);
        content.addView(playerSubtitle, lp(-1, -2, 0, 18));
        playerStatus = centred("", 19, GREEN, true);
        playerStatus.setId(R.id.playback_status);
        playerStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(playerStatus, lp(-1, -2, 0, 18));
        playerFavourite = ui.button("", TEXT, BACKGROUND);
        playerFavourite.setId(R.id.player_favourite);
        playerFavourite.setCompoundDrawablePadding(ui.dp(8));
        ui.clickable(playerFavourite, BACKGROUND, 10, BORDER);
        playerFavourite.setOnClickListener(v -> { if (selected != null) toggleFavourite(selected); });
        content.addView(playerFavourite, lp(-2, -2, 0, 26));

        LinearLayout controls = ui.row();
        controls.setGravity(Gravity.CENTER);
        previous = ui.iconButton(R.drawable.ic_ui_previous, GREEN, SAGE, 40, 64, getString(R.string.previous_station));
        previous.setId(R.id.previous_station);
        previous.setOnClickListener(v -> skip(-1));
        controls.addView(labelledControl(previous, getString(R.string.previous), 64), new LinearLayout.LayoutParams(0, -2, 1));
        playerToggle = ui.iconButton(R.drawable.ic_ui_play, Color.WHITE, GREEN, 64, 108, "");
        playerToggle.setId(R.id.stop_button);
        playerToggle.setOnClickListener(v -> togglePlayback());
        LinearLayout middle = ui.column();
        middle.setGravity(Gravity.CENTER_HORIZONTAL);
        middle.addView(playerToggle, lp(ui.dp(108), ui.dp(108), 0, 8));
        toggleLabel = centred("", 22, TEXT, true);
        middle.addView(toggleLabel, lp(-1, -2, 0, 0));
        controls.addView(middle, new LinearLayout.LayoutParams(0, -2, 1.25f));
        next = ui.iconButton(R.drawable.ic_ui_next, GREEN, SAGE, 40, 64, getString(R.string.next_station));
        next.setId(R.id.next_station);
        next.setOnClickListener(v -> skip(1));
        controls.addView(labelledControl(next, getString(R.string.next), 64), new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(controls, lp(-1, -2, 0, 24));
        queueLabel = centred("", 17, MUTED, false);
        queueLabel.setId(R.id.queue_position);
        content.addView(queueLabel, lp(-1, -2, 0, 0));
    }

    private View labelledControl(ImageButton control, String label, int size) {
        LinearLayout column = ui.column();
        column.setGravity(Gravity.CENTER_HORIZONTAL);
        column.addView(control, lp(ui.dp(size), ui.dp(size), 0, 8));
        TextView text = centred(label, 17, TEXT, false);
        text.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        column.addView(text, lp(-1, -2, 0, 0));
        return column;
    }

    private void showStatus(String message) {
        playback = PlaybackPresentation.from(message);
        if (playback.station != null) {
            selected = playback.station;
            preferences.edit().putString("selected", selected.name).apply();
            ensureQueue();
        }
        updatePlaybackViews();
    }

    private void updatePlaybackViews() {
        if (miniBar == null || playerStatus == null) return;
        for (Map.Entry<String, RowViews> entry : rows.entrySet()) {
            boolean active = selected != null && entry.getKey().equals(selected.name) && playback.active;
            RowViews row = entry.getValue();
            row.row.setBackground(ui.surface(active ? SAGE : Color.TRANSPARENT, 10, null));
            row.play.setText(getString(active ? (playback.live ? R.string.playing : playback.label) : R.string.play_button));
            row.play.setTextSize(active ? 15 : 17);
            row.play.setTextColor(active ? GREEN : Color.WHITE);
            row.play.setCompoundDrawables(active ? null : ui.icon(R.drawable.ic_ui_play, Color.WHITE, 18), null, null, null);
            ui.clickable(row.play, active ? Color.TRANSPARENT : GREEN, 8, null);
            row.play.setContentDescription(getString(active ? R.string.open_player_accessibility : R.string.play_accessibility, entry.getKey()));
        }
        miniBar.setVisibility(selected == null ? View.GONE : View.VISIBLE);
        updateBrowserPadding();
        boolean loading = selected != null && playback.active && !playback.live;
        miniLoading.setLoading(loading);
        playerLoading.setLoading(loading);
        if (selected == null) return;
        ui.setLogo(miniLogo, selected, 6);
        miniName.setText(selected.name);
        openPlayer.setContentDescription(getString(R.string.open_player_accessibility, selected.name)
            + ". " + getString(playback.label));
        // An error must also be visible when the user has returned to the browser.
        miniAction.setText(playback.error ? getString(R.string.unavailable) : getString(R.string.open_player));
        miniAction.setTextColor(playback.error ? RED : GREEN);
        configureToggle(miniToggle);
        configureToggle(playerToggle);
        ui.setLogo(playerLogo, selected, 36);
        playerName.setText(selected.name);
        playerSubtitle.setText(StationPresentation.subtitle(this, selected));
        playerStatus.setText(playback.live ? getString(R.string.live_status, getString(playback.label)) : getString(playback.label));
        playerStatus.setTextColor(playback.error ? RED : (playback.active ? GREEN : MUTED));
        boolean saved = favourites.contains(selected.name);
        playerFavourite.setText(saved ? R.string.saved : R.string.save);
        playerFavourite.setCompoundDrawables(ui.icon(saved ? R.drawable.ic_ui_heart : R.drawable.ic_ui_heart_outline, GREEN, 26), null, null, null);
        playerFavourite.setContentDescription(getString(saved ? R.string.remove_station : R.string.save_station, selected.name));
        setCheckedAccessibility(playerFavourite, saved);
        toggleLabel.setText(playback.active ? R.string.stop_button : R.string.play_button);
        ensureQueue();
        int position = queue.indexOf(selected);
        queueLabel.setText(getString(R.string.queue_position,
            getString(queueFavourites ? R.string.favourites : R.string.all_stations), position + 1, queue.size()));
        previous.setEnabled(queue.size() > 1);
        next.setEnabled(queue.size() > 1);
        previous.setAlpha(queue.size() > 1 ? 1f : 0.35f);
        next.setAlpha(queue.size() > 1 ? 1f : 0.35f);
    }

    private void configureToggle(ImageButton button) {
        button.setImageDrawable(ui.icon(playback.active ? R.drawable.ic_ui_stop : R.drawable.ic_ui_play, Color.WHITE, 24));
        ui.clickable(button, playback.active ? RED : GREEN, 100, null);
        button.setContentDescription(getString(playback.active ? R.string.stop_accessibility : R.string.play_accessibility, selected.name));
    }

    private void togglePlayback() {
        if (selected == null) return;
        if (playback.active) {
            startService(new Intent(this, RadioService.class).setAction(RadioService.ACTION_STOP));
            showStatus("Stopped");
        } else playStation(selected, false);
    }

    private void playStation(Station station, boolean openFocus) {
        selected = station;
        preferences.edit().putString("selected", station.name).apply();
        ensureQueue();
        showStatus("Connecting to " + station.name + "…");
        Intent intent = new Intent(this, RadioService.class).setAction(RadioService.ACTION_PLAY)
            .putExtra(RadioService.EXTRA_URL, station.url).putExtra(RadioService.EXTRA_NAME, station.name);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent); else startService(intent);
        if (openFocus) setPlayerOpen(true);
    }

    private void captureQueue() {
        queue.clear();
        queue.addAll(visibleStations);
        queueFavourites = favouritesTab;
        persistQueue();
    }

    private void ensureQueue() {
        if (selected != null && !queue.contains(selected)) {
            queue.clear();
            queue.addAll(StationData.ALL);
            queueFavourites = false;
            persistQueue();
        }
    }

    private void persistQueue() {
        JSONArray names = new JSONArray();
        for (Station station : queue) names.put(station.name);
        preferences.edit().putString("queue", names.toString()).putBoolean("queue_favourites", queueFavourites).apply();
    }

    private void restoreQueue() {
        queueFavourites = preferences.getBoolean("queue_favourites", false);
        try {
            JSONArray names = new JSONArray(preferences.getString("queue", "[]"));
            for (int i = 0; i < names.length(); i++) {
                Station station = StationPresentation.named(names.optString(i));
                if (station != null && !queue.contains(station)) queue.add(station);
            }
        } catch (JSONException ignored) { queue.clear(); }
    }

    private void skip(int direction) {
        if (selected == null || queue.size() < 2) return;
        int index = (queue.indexOf(selected) + direction + queue.size()) % queue.size();
        playStation(queue.get(index), false);
    }

    private void toggleFavourite(Station station) {
        if (!favourites.add(station.name)) favourites.remove(station.name);
        preferences.edit().putStringSet("favourites", new HashSet<>(favourites)).apply();
        renderStations();
    }

    void setStationFilter(int index) {
        if (index < 0 || index > Station.Language.values().length) return;
        filterIndex = index;
        renderStations();
    }

    private void showStationLanguages() {
        String[] labels = new String[StationPresentation.LANGUAGE_LABELS.length + 1];
        labels[0] = getString(R.string.all_languages);
        for (int i = 1; i < labels.length; i++) labels[i] = getString(StationPresentation.LANGUAGE_LABELS[i - 1]);
        new AlertDialog.Builder(this).setTitle(R.string.filter_language)
            .setSingleChoiceItems(labels, filterIndex, (dialog, index) -> { setStationFilter(index); dialog.dismiss(); })
            .setNegativeButton(android.R.string.cancel, null).show();
    }

    private int currentLanguageIndex() {
        String tag = preferences.getString("language", "en");
        for (int i = 0; i < LOCALE_TAGS.length; i++) if (LOCALE_TAGS[i].equals(tag)) return i;
        return 0;
    }

    private String currentLanguageName() { return LOCALE_NAMES[currentLanguageIndex()]; }

    private void showAppLanguages() {
        new AlertDialog.Builder(this).setTitle(R.string.app_language)
            .setSingleChoiceItems(LOCALE_NAMES, currentLanguageIndex(), (dialog, index) -> {
                dialog.dismiss();
                if (index != currentLanguageIndex()) {
                    preferences.edit().putString("language", LOCALE_TAGS[index]).apply();
                    recreate();
                }
            }).setNegativeButton(android.R.string.cancel, null).show();
    }

    private void setPlayerOpen(boolean open) {
        playerOpen = open && selected != null;
        if (playerOpen) hideKeyboard();
        playerTransition.show(playerOpen);
        updateLoadingMotion();
        if (Build.VERSION.SDK_INT >= 33) {
            if (backCallback == null) backCallback = () -> setPlayerOpen(false);
            if (playerOpen && !backRegistered) {
                getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT, backCallback);
                backRegistered = true;
            } else if (!playerOpen && backRegistered) {
                getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
                backRegistered = false;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override public void onBackPressed() {
        if (playerOpen) setPlayerOpen(false); else super.onBackPressed();
    }

    private void hideKeyboard() {
        search.clearFocus();
        getSystemService(InputMethodManager.class).hideSoftInputFromWindow(search.getWindowToken(), 0);
    }

    private void setCheckedAccessibility(View view, boolean checked) {
        view.setSelected(checked);
        view.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setCheckable(true);
                info.setChecked(checked);
            }
        });
    }

    private void hideDescendantsFromAccessibility(ViewGroup group) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
            if (child instanceof ViewGroup) hideDescendantsFromAccessibility((ViewGroup) child);
        }
    }

    private TextView centred(String text, int size, int color, boolean bold) {
        TextView view = ui.text(text, size, color, bold);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private LinearLayout.LayoutParams lp(int width, int height, int top, int bottom) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.topMargin = ui.dp(top);
        params.bottomMargin = ui.dp(bottom);
        return params;
    }

    @Override protected void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(this, receiver, new IntentFilter(RadioService.BROADCAST_STATUS),
            ContextCompat.RECEIVER_NOT_EXPORTED);
        showStatus(RadioService.getLatestStatus());
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putInt("filter", filterIndex);
        state.putBoolean("favourites_tab", favouritesTab);
        state.putString("query", query);
        state.putBoolean("player_open", playerOpen);
        state.putInt("browser_scroll", scroller.getScrollY());
        state.putInt("player_scroll", focus.getScrollY());
        super.onSaveInstanceState(state);
    }

    @Override protected void onResume() {
        super.onResume();
        resumed = true;
        playerTransition.onResume();
        updateLoadingMotion();
    }

    @Override protected void onPause() {
        resumed = false;
        updateLoadingMotion();
        playerTransition.onPause();
        super.onPause();
    }

    private void updateLoadingMotion() {
        // Both surfaces are briefly visible during a transition; animate only the destination's ring.
        miniLoading.setMotionAllowed(resumed && !playerOpen);
        playerLoading.setMotionAllowed(resumed && playerOpen);
    }

    @Override protected void onStop() {
        unregisterReceiver(receiver);
        super.onStop();
    }

    @Override protected void onDestroy() {
        playerTransition.onPause();
        if (Build.VERSION.SDK_INT >= 33 && backRegistered)
            getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback(backCallback);
        super.onDestroy();
    }
}
