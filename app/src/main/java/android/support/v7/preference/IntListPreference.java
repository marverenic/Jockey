package android.support.v7.preference;

import android.content.Context;
import android.util.AttributeSet;

public class IntListPreference extends ListPreference {

    public IntListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public IntListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public IntListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public IntListPreference(Context context) {
        super(context);
    }

    @Override
    protected boolean persistString(String value) {
        return persistInt(Integer.parseInt(value));
    }

    @Override
    protected String getPersistedString(String defaultReturnValue) {
        int defaultIntReturnValue;

        if (defaultReturnValue == null) {
            defaultIntReturnValue = 0;
        } else {
            defaultIntReturnValue = Integer.parseInt(defaultReturnValue);
        }

        return Integer.toString(getPersistedInt(defaultIntReturnValue));
    }
}
