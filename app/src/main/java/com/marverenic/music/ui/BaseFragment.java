package com.marverenic.music.ui;

import android.app.Activity;

import com.trello.rxlifecycle.components.support.RxFragment;

public abstract class BaseFragment extends RxFragment {

    /**
     * If this fragment is attached to a {@link BaseActivity}, then this callback will be triggered
     * when the back button is pressed. If multiple BaseFragments are visible in a single activity,
     * there is no guarantee on which fragment will get the callback first.
     * @return Whether or not this event was consumed. If false, the activity will handle the event.
     * @see Activity#onBackPressed()
     */
    protected boolean onBackPressed() {
        return false;
    }

}
