package com.marverenic.music.adapter;

import android.support.v7.widget.RecyclerView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.MeasurableAdapter;

public class HeterogeneousFastScrollAdapter extends HomogeneousFastScrollAdapter
        implements MeasurableAdapter {

    @Override
    public int getViewTypeHeight(RecyclerView recyclerView, int viewType) {
        for (int i = 0; i < getSectionCount(); i++) {
            Section section = getSection(i);
            if (section.getTypeId() == viewType) {
                if (!(section instanceof MeasurableAdapter)) {
                    return 0;
                }

                return ((MeasurableAdapter) section).getViewTypeHeight(recyclerView, 0);
            }
        }
        return 0;
    }

}
