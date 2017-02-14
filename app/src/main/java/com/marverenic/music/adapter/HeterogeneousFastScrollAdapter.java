package com.marverenic.music.adapter;

import android.support.annotation.NonNull;

import com.marverenic.heterogeneousadapter.Coordinate;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter;

public class HeterogeneousFastScrollAdapter extends HeterogeneousAdapter
        implements SectionedAdapter {

    private Coordinate mSectionCoordinate = new Coordinate();

    @Override
    public HeterogeneousAdapter addSection(@NonNull Section section, int index) {
        if (section instanceof SectionedAdapter) {
            return super.addSection(section, index);
        } else {
            throw new IllegalArgumentException("Cannot add a Section that does not implement the " +
                    "SectionedAdapter interface");
        }
    }

    @NonNull
    @Override
    public String getSectionName(int position) {
        lookupCoordinates(position, mSectionCoordinate);
        Section<?> section = getSection(mSectionCoordinate.getSection());

        if (section instanceof SectionedAdapter) {
            SectionedAdapter sectionedAdapter = (SectionedAdapter) section;
            return sectionedAdapter.getSectionName(mSectionCoordinate.getItemIndex());
        } else {
            throw new IllegalStateException("One of the attached Sections does not implement " +
                    "the SectionedAdapter interface (index = " + position);
        }
    }
}
