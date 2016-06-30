package com.marverenic.music.instances.section;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.marverenic.music.R;
import com.marverenic.heterogeneousadapter.EnhancedViewHolder;
import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;

public class SpacerSingleton extends HeterogeneousAdapter.SingletonSection<Void> {

    public static final int ID = 7774;
    public static final int ALWAYS_SHOWN = -1;

    private int mLinkedTypeId;
    private int mHeight;

    public SpacerSingleton(int linkedTypeId, int height) {
        super(ID, null);
        mLinkedTypeId = linkedTypeId;
        mHeight = height;
    }

    public void setHeight(int height) {
        mHeight = height;
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
        View itemView = LayoutInflater
                        .from(parent.getContext())
                        .inflate(R.layout.instance_blank, parent, false);


        return new ViewHolder(itemView, (RecyclerView) parent);
    }

    public class ViewHolder extends EnhancedViewHolder<Void> {

        RecyclerView parent;

        public ViewHolder(View itemView, RecyclerView parent) {
            super(itemView);
            this.parent = parent;
        }

        @Override
        public void update(Void item, int sectionPosition) {
            ViewGroup.MarginLayoutParams params =
                    (ViewGroup.MarginLayoutParams) itemView.getLayoutParams();

            int height = SpacerSingleton.this.mHeight;
            if (height >= 0) {
                params.height = height;
            } else {
                params.height = parent.getHeight() + height;
            }
            itemView.setLayoutParams(params);
        }
    }
}
