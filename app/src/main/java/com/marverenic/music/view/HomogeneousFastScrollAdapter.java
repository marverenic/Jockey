package com.marverenic.music.view;

import android.support.annotation.NonNull;

import com.marverenic.adapter.Coordinate;
import com.marverenic.adapter.HeterogeneousAdapter;
import com.simplecityapps.recyclerview_fastscroll.views.FastScrollRecyclerView.SectionedAdapter;

public class HomogeneousFastScrollAdapter extends HeterogeneousAdapter implements SectionedAdapter {

    private static final int LAST_SECTION_INDEX = -1;
    private static final int FIRST_SECTION_INDEX = -2;

    private Coordinate mSectionCoordinate = new Coordinate();

    @NonNull
    @Override
    public String getSectionName(int position) {
        if (getDataSize() == 0) {
            return "";
        }

        lookupCoordinates(position, mSectionCoordinate);
        Section<?> section = getSection(mSectionCoordinate.getSection());

        /*
            If the section at the position doesn't implement SectionedAdapter, linearly walk up
            and down sections to find the nearest section that does implement the interface, and
            use the adjacent section name instead
         */
        int walk = -1;
        while (!(section instanceof SectionedAdapter)
                && mSectionCoordinate.getSection() + walk >= 0
                && mSectionCoordinate.getSection() + walk < getSectionCount()) {

            section = getSection(mSectionCoordinate.getSection() + walk);
            if (walk > 0) {
                mSectionCoordinate.setItemIndex(LAST_SECTION_INDEX);
                walk = -(walk + 1);
            } else {
                mSectionCoordinate.setItemIndex(FIRST_SECTION_INDEX);
                walk = -(walk - 1);
            }
        }

        if (mSectionCoordinate.getSection() + walk >= 0) {
            while (!(section instanceof SectionedAdapter)
                    && mSectionCoordinate.getSection() + walk >= 0) {
                mSectionCoordinate.setItemIndex(LAST_SECTION_INDEX);
                walk--;
                section = getSection(mSectionCoordinate.getSection() + walk);
            }
        } else if (mSectionCoordinate.getSection() + walk < getSectionCount()) {
            while (!(section instanceof SectionedAdapter)
                    && mSectionCoordinate.getSection() + walk < getSectionCount()) {
                mSectionCoordinate.setItemIndex(FIRST_SECTION_INDEX);
                walk++;
                section = getSection(mSectionCoordinate.getSection() + walk);
            }
        }

        if (section instanceof SectionedAdapter) {
            SectionedAdapter sectionedAdapter = (SectionedAdapter) section;
            int index = mSectionCoordinate.getItemIndex();

            if (index == FIRST_SECTION_INDEX) {
                index = 0;
            } else if (index == LAST_SECTION_INDEX) {
                index = section.getItemCount(this) - 1;
            }

            return sectionedAdapter.getSectionName(index);
        } else {
            return "";
        }
    }

}
