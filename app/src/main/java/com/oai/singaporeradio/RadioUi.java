package com.oai.singaporeradio;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/** Shared presentation styles; playback and the station catalog are deliberately separate. */
final class RadioUi {
    static final int BACKGROUND = 0xFFFFFEFA, TEXT = 0xFF101820, MUTED = 0xFF56616B;
    static final int GREEN = 0xFF07583F, RED = 0xFFB7352D, SAGE = 0xFFEDF4EE;
    static final int BORDER = 0xFFB3BDB8, BAR = 0xEBE4EEE6;
    private final Context context;

    RadioUi(Context context) { this.context = context; }
    int dp(int value) { return Math.round(value * context.getResources().getDisplayMetrics().density); }

    LinearLayout column() {
        LinearLayout view = new LinearLayout(context);
        view.setOrientation(LinearLayout.VERTICAL);
        return view;
    }

    LinearLayout row() {
        LinearLayout view = new LinearLayout(context);
        view.setGravity(Gravity.CENTER_VERTICAL);
        return view;
    }

    TextView text(CharSequence text, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(text);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        view.setIncludeFontPadding(false);
        return view;
    }

    GradientDrawable surface(int color, int radius, Integer border) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(radius));
        if (border != null) shape.setStroke(dp(1), border);
        return shape;
    }

    void clickable(View view, int color, int radius, Integer border) {
        view.setBackground(new RippleDrawable(ColorStateList.valueOf(0x2207583F),
            surface(color, radius, border), surface(Color.WHITE, radius, null)));
        view.setFocusable(true);
    }

    Drawable icon(int resource, int color, int size) {
        Drawable icon = context.getDrawable(resource).mutate();
        icon.setTint(color);
        icon.setBounds(0, 0, dp(size), dp(size));
        return icon;
    }

    ImageView image(int resource, int size, int color) {
        ImageView view = new ImageView(context);
        view.setImageDrawable(icon(resource, color, size));
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return view;
    }

    ImageView logo(Station station, int radius) {
        ImageView logo = new ImageView(context);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        setLogo(logo, station, radius);
        logo.setClipToOutline(true);
        logo.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return logo;
    }

    void setLogo(ImageView logo, Station station, int radius) {
        logo.setImageResource(StationPresentation.logo(station));
        // The bundled YES 933 asset is yellow on transparency and needs a dark surface.
        int background = station != null && station.name.equals("YES 933") ? TEXT : Color.WHITE;
        logo.setBackground(surface(background, radius, null));
    }

    Button button(String text, int color, int background) {
        Button view = new Button(context);
        view.setAllCaps(false);
        view.setText(text);
        view.setTextSize(17);
        view.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        view.setTextColor(color);
        view.setGravity(Gravity.CENTER);
        view.setMinWidth(0);
        view.setMinimumWidth(0);
        view.setMinHeight(dp(52));
        view.setMinimumHeight(dp(52));
        view.setPadding(dp(12), dp(10), dp(12), dp(10));
        view.setStateListAnimator(null);
        view.setBackgroundTintList(null);
        clickable(view, background, 8, null);
        return view;
    }

    ImageButton iconButton(int icon, int foreground, int background, int glyphSize, int touchSize, String description) {
        ImageButton view = new ImageButton(context);
        // Set padding before layout so ImageView calculates its scale and centre
        // against the final content area, including when a hidden player reappears.
        view.setImageDrawable(icon(icon, foreground, 24));
        view.setScaleType(ImageView.ScaleType.FIT_CENTER);
        view.setContentDescription(description);
        view.setMinimumWidth(dp(touchSize));
        view.setMinimumHeight(dp(touchSize));
        int padding = dp(Math.max(0, (touchSize - glyphSize) / 2));
        view.setPadding(padding, padding, padding, padding);
        clickable(view, background, 100, null);
        return view;
    }
}
