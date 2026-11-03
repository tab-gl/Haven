package com.tabgl.haven;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextClock;

/** Native platform clock embedded in the Compose home screen. */
public final class HavenClockView extends LinearLayout {
    public HavenClockView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_YES);
        setContentDescription("Current time and date");

        int[] attrs = { android.R.attr.textColorPrimary, android.R.attr.textColorSecondary };
        TypedArray colors = context.obtainStyledAttributes(attrs);
        int primary = colors.getColor(0, 0xFF191B18);
        int secondary = colors.getColor(1, 0xFF62666D);
        colors.recycle();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            boolean dark = (getResources().getConfiguration().uiMode
                    & android.content.res.Configuration.UI_MODE_NIGHT_MASK)
                    == android.content.res.Configuration.UI_MODE_NIGHT_YES;
            primary = getResources().getColor(
                    dark ? android.R.color.system_neutral1_100 : android.R.color.system_neutral1_900,
                    context.getTheme());
            secondary = getResources().getColor(
                    dark ? android.R.color.system_neutral2_300 : android.R.color.system_neutral2_600,
                    context.getTheme());
        }

        TextClock time = new TextClock(context);
        time.setFormat12Hour("h:mm");
        time.setFormat24Hour("HH:mm");
        time.setTextColor(primary);
        time.setTextSize(TypedValue.COMPLEX_UNIT_SP, 66);
        time.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        time.setLetterSpacing(-0.075f);
        time.setIncludeFontPadding(false);
        time.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        addView(time, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

        TextClock date = new TextClock(context);
        date.setFormat12Hour("EEEE, d MMMM");
        date.setFormat24Hour("EEEE, d MMMM");
        date.setTextColor(secondary);
        date.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        date.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        date.setIncludeFontPadding(false);
        LayoutParams dateParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = dp(3);
        addView(date, dateParams);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
