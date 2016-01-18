package com.marverenic.music.instances.section;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.R;
import com.marverenic.music.view.EnhancedAdapters.EnhancedViewHolder;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

public class SpacerSingleton extends HeterogeneousAdapter.SingletonSection<Void> {

    public static final int ID = 7774;
    public static final int ALWAYS_SHOWN = -1;

    private int mLinkedTypeId;

    public SpacerSingleton(int linkedTypeId) {
        super(ID, null);
        mLinkedTypeId = linkedTypeId;
    }

    @Override
    public boolean showSection(HeterogeneousAdapter adapter) {
        if (mLinkedTypeId == ALWAYS_SHOWN) {
            return true;
        } else {
            HeterogeneousAdapter.Section dependency = adapter.getSectionById(mLinkedTypeId);
            return dependency == null || dependency.getSize(adapter) > 0;
        }
    }

    @Override
    public EnhancedViewHolder<Void> createViewHolder(HeterogeneousAdapter adapter,
                                                                  ViewGroup parent) {
        return new ViewHolder(
                LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.instance_blank, parent, false));
    }

    public class ViewHolder extends EnhancedViewHolder<Void> {

        public ViewHolder(View itemView) {
            super(itemView);
        }

        @Override
        public void update(Void item, int sectionPosition) {

        }
    }
}
