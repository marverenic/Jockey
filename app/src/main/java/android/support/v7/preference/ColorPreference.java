package android.support.v7.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.GradientDrawable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.widget.ImageView;

import com.marverenic.music.R;

public class ColorPreference extends IntListPreference {

    private static final int STROKE_THICKNESS_DP = 2;
    private static final int[] ATTRIBUTES = {R.attr.colorEntries};

    private ImageView mColorPreview;
    private GradientDrawable mColorDrawable;
    private int mStrokeThickness;
    private int[] mColors;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_color_widget);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, ATTRIBUTES, 0, 0);

        int arrayRes = a.getResourceId(0, 0);
        mColors = context.getResources().getIntArray(arrayRes);

        a.recycle();

        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        mStrokeThickness = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                STROKE_THICKNESS_DP, metrics);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mColorPreview = (ImageView) holder.findViewById(R.id.pref_color_preview);
        mColorDrawable = new GradientDrawable();
        mColorDrawable.setShape(GradientDrawable.OVAL);

        setColor(mColors[findIndexOfValue(getValue())]);
    }

    private void setColor(@ColorInt int color) {
        int rgb = 0xFFFFFF & color; // discard alpha bits
        int centerColor = (0xFF << 24) | rgb; // 100% opaque
        int strokeColor = (0x80 << 24) | rgb; // 50% transparent

        mColorDrawable.setColor(centerColor);
        mColorDrawable.setStroke(mStrokeThickness, strokeColor);

        mColorPreview.setImageDrawable(mColorDrawable);
    }
}
