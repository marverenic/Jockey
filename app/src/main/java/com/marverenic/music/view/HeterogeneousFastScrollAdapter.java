package com.marverenic.music.view;

import android.support.v7.widget.RecyclerView;

import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.MeasurableAdapter;

public class HeterogeneousFastScrollAdapter extends HomogeneousFastScrollAdapter
        implements MeasurableAdapter<RecyclerView.ViewHolder> {

    @Override
    public int getViewTypeHeight(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, int viewType) {
        for (int i = 0; i < getSectionCount(); i++) {
            Section section = getSection(i);
            if (section.getTypeId() == viewType) {
                if (!(section instanceof MeasurableSection)) {
                    return 0;
                }

                return ((MeasurableSection) section).getViewTypeHeight(recyclerView);
            }
        }
        return 0;
    }
}
