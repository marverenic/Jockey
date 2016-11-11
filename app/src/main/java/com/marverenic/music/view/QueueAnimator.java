package com.marverenic.music.view;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;

import com.marverenic.music.instances.section.QueueSection;
import com.marverenic.music.instances.section.SpacerSingleton;

public class QueueAnimator extends DefaultItemAnimator {

    public QueueAnimator() {
        setSupportsChangeAnimations(false);
    }

    @Override
    public boolean animateAdd(RecyclerView.ViewHolder holder) {
        if (shouldAnimateAdd(holder)) {
            return super.animateAdd(holder);
        } else {
            dispatchAddFinished(holder);
            return false;
        }
    }

    private boolean shouldAnimateAdd(RecyclerView.ViewHolder holder) {
        if (holder instanceof SpacerSingleton.ViewHolder) {
            return false;
        } else if (holder instanceof QueueSection.ViewHolder) {
            return false;
        }
        return true;
    }

    @Override
    public boolean animateRemove(RecyclerView.ViewHolder holder) {
        if (shouldAnimateRemove(holder)) {
            return super.animateRemove(holder);
        } else {
            dispatchRemoveFinished(holder);
            return false;
        }
    }

    private boolean shouldAnimateRemove(RecyclerView.ViewHolder holder) {
        if (holder instanceof SpacerSingleton.ViewHolder) {
            return true;
        } else if (holder instanceof QueueSection.ViewHolder) {
            return ((QueueSection.ViewHolder) holder).isRemoved();
        }
        return true;
    }
}
