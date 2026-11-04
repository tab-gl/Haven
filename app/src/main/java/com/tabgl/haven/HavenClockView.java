package com.tabgl.haven;

import android.content.Context;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextClock;

/**
 * Native platform clock embedded in the Compose home screen. TextClock ticks itself without any
 * Compose recomposition, which keeps the home screen idle-cost near zero. Colours are pushed in
 * from the active Haven theme via {@link #setColors(int, int)}.
 */
public final class HavenClockView extends LinearLayout {
    private final TextClock time;
    private final TextClock date;

    public HavenClockView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("Current time and date. Opens the clock.");

        time = new TextClock(context);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 66);
        time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        time.setLetterSpacing(-0.075f);
        time.setIncludeFontPadding(false);
        time.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        addView(time, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        date = new TextClock(context);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        date.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        date.setIncludeFontPadding(false);
        LayoutParams dateParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = Math.round(3 * getResources().getDisplayMetrics().density);
        addView(date, dateParams);

        setColors(0xFFE5E2DC, 0xFFBEC8C4);
    }

    /** Applies the launcher theme's colours (ARGB ints) to the time and date text. */
    public void setColors(int primary, int secondary) {
        time.setTextColor(primary);
        date.setTextColor(secondary);
    }
}
