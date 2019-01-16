package com.marverenic.music.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.TextView;

import com.marverenic.music.R;

import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

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
        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.TimeView, 0, 0);

        try {
            setTime(a.getInt(R.styleable.TimeView_time, 0));
        } finally {
            a.recycle();
        }
    }

    /**
     * Update this TextView to display a formatted time as mm:ss
     * @param time The time to format and display (in milliseconds)
     */
    public void setTime(int time) {
        setText(getValue(getContext(), time));
    }

    public static String getValue(Context context, int time) {
        int sec = (int) TimeUnit.SECONDS.convert(time, TimeUnit.MILLISECONDS) % 60;
        int min = (int) TimeUnit.MINUTES.convert(time, TimeUnit.MILLISECONDS) % 60;
        int hours = (int) TimeUnit.HOURS.convert(time, TimeUnit.MILLISECONDS);

        if (hours == 0) {
            return context.getString(R.string.format_time, Integer.toString(min),
                    DIGIT_FORMAT.format(sec));
        } else {
            return context.getString(R.string.format_time_hours, Integer.toString(hours),
                    DIGIT_FORMAT.format(min), DIGIT_FORMAT.format(sec));
        }
    }
}
