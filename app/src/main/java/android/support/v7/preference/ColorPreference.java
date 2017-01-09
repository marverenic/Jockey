package android.support.v7.preference;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.marverenic.music.R;

public class ColorPreference extends IntListPreference {

    private static final int[] ATTRIBUTES = {R.attr.colorEntries};

    private ImageView mColorPreview;
    private Drawable mColorDrawable;
    private int[] mColors;

    public ColorPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWidgetLayoutResource(R.layout.preference_color_widget);

        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, ATTRIBUTES, 0, 0);

        int arrayRes = a.getResourceId(0, 0);
        mColors = context.getResources().getIntArray(arrayRes);

        a.recycle();
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        mColorPreview = (ImageView) holder.findViewById(R.id.pref_color_preview);
        mColorDrawable = mColorPreview.getDrawable();

        setColor(mColors[findIndexOfValue(getValue())]);
    }

    private void setColor(@ColorInt int color) {
        mColorDrawable.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        mColorPreview.setImageDrawable(mColorDrawable);
    }
}
