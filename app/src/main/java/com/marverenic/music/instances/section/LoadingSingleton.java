package com.marverenic.music.instances.section;

import android.support.annotation.ColorInt;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.marverenic.music.R;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.view.MaterialProgressDrawable;

public class LoadingSingleton extends HeterogeneousAdapter.SingletonSection<Void> {

    public static final int ID = 79;
    private int[] mColors;

    public LoadingSingleton(@ColorInt int... colors) {
        super(ID, null);
        mColors = colors;
    }

    @Override
    public EnhancedViewHolder<Void> createViewHolder(HeterogeneousAdapter adapter,
                                                     ViewGroup parent) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.instance_loading, parent, false));
    }

    private class ViewHolder extends EnhancedViewHolder<Void> {

        private MaterialProgressDrawable spinner;

        ViewHolder(View itemView) {
            super(itemView);
            ImageView spinnerView = (ImageView) itemView.findViewById(R.id.loadingDrawable);
            spinner = new MaterialProgressDrawable(itemView.getContext(), spinnerView);
            spinner.setColorSchemeColors(mColors);
            spinner.updateSizes(MaterialProgressDrawable.LARGE);
            spinner.setAlpha(255);
            spinnerView.setImageDrawable(spinner);
            spinner.start();
        }

        @Override
        public void update(Void item, int sectionPosition) {
            spinner.stop();
            spinner.start();
        }
    }
}
