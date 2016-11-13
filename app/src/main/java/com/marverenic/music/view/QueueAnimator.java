package com.marverenic.music.view;

import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView.ViewHolder;

import com.marverenic.music.instances.section.QueueSection;
import com.marverenic.music.instances.section.SpacerSingleton;

public class QueueAnimator extends DefaultItemAnimator {

    public QueueAnimator() {
        setSupportsChangeAnimations(false);
    }

    @Override
    public boolean animateAdd(ViewHolder holder) {
        if (shouldAnimateAdd(holder)) {
            return super.animateAdd(holder);
        } else {
            dispatchAddFinished(holder);
            return false;
        }
    }

    private boolean shouldAnimateAdd(ViewHolder holder) {
        if (holder instanceof SpacerSingleton.ViewHolder) {
            return false;
        } else if (holder instanceof QueueSection.ViewHolder) {
            return true;
        }
        return false;
    }

    @Override
    public boolean animateRemove(ViewHolder holder) {
        if (shouldAnimateRemove(holder)) {
            return super.animateRemove(holder);
        } else {
            dispatchRemoveFinished(holder);
            return false;
        }
    }

    private boolean shouldAnimateRemove(ViewHolder holder) {
        if (holder instanceof SpacerSingleton.ViewHolder) {
            return false;
        } else if (holder instanceof QueueSection.ViewHolder) {
            return true;
        }
        return true;
    }
}
