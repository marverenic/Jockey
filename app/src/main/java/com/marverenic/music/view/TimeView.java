package com.marverenic.music.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

import com.marverenic.music.R;

import java.text.DecimalFormat;

/**
 * An extension of TextView used to display times
 * @see #setTime(int)
 */
public class TimeView extends TextView {

    private static final DecimalFormat DIGIT_FORMAT = new DecimalFormat("00");

    public TimeView(Context context) {
        super(context);
    }

    public TimeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TimeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @TargetApi(21)
    public TimeView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Update this TextView to display a formatted time as mm:ss
     * @param time The time to format and display (in milliseconds)
     */
    public void setTime(int time) {
        setText(getValue(getContext(), time));
    }

    public static String getValue(Context context, int time) {
        time /= 1000;
        int sec = time % 60;
        int min = time / 60 % 60;

        return context.getString(R.string.format_time, min, DIGIT_FORMAT.format(sec));
    }
}
