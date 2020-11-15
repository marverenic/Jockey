package androidx.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.TextView;

import androidx.core.view.GravityCompat;

/**
 * PreferenceCategory fix which allows one to use multiple themes. The original
 * "preference_fallback_accent_color" override would not allow this as it is not modifiable during
 * runtime.
 * If you use this class in your preference XML, you don't have to redefine
 * "preference_fallback_accent_color".
 *
 *
 * Copied from https://github.com/Gericop/Android-Support-Preference-V7-Fix
 */
public class PreferenceCategoryCompat extends PreferenceCategory {
    private static final int PADDING_DP = 14;
    private static final int[] COLOR_ACCENT_ID = new int[]{androidx.appcompat.R.attr.colorAccent};

    public PreferenceCategoryCompat(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setViewId(android.R.id.title);
    }

    public PreferenceCategoryCompat(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setViewId(android.R.id.title);
    }

    public PreferenceCategoryCompat(Context context, AttributeSet attrs) {
        super(context, attrs);
        setViewId(android.R.id.title);
    }

    public PreferenceCategoryCompat(Context context) {
        super(context);
        setViewId(android.R.id.title);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return;

        final TextView titleView = (TextView) holder.findViewById(android.R.id.title);

        if (titleView != null) {
            titleView.setGravity(GravityCompat.START | Gravity.CENTER_VERTICAL);
            final TypedArray typedArray = getContext().obtainStyledAttributes(COLOR_ACCENT_ID);

            try {
                if (typedArray.length() > 0) {
                    final int accentColor = typedArray.getColor(0, 0xff4081); // defaults to pink
                    titleView.setTextColor(accentColor);
                }
            } finally {
                typedArray.recycle();
            }

            float density = getContext().getResources().getDisplayMetrics().density;
            int paddingPx = (int) (density * PADDING_DP);

            // View already includes bottom margin. Add padding to all other sides for consistency
            // across Android versions.
            titleView.setPadding(paddingPx, paddingPx, paddingPx, 0);
        }
    }
}
