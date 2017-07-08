package com.marverenic.music.ui;

import android.os.Bundle;
import android.support.annotation.IdRes;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.marverenic.music.R;

public abstract class SingleFragmentActivity extends BaseActivity {

    private static final String CONTENT_FRAGMENT_TAG = "content_fragment";

    protected abstract Fragment onCreateFragment(Bundle savedInstanceState);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateLayout(savedInstanceState);

        if (getSupportFragmentManager().findFragmentByTag(CONTENT_FRAGMENT_TAG) == null) {
            Fragment fragment = onCreateFragment(savedInstanceState);
            getSupportFragmentManager().beginTransaction()
                    .add(getFragmentContainerId(), fragment, CONTENT_FRAGMENT_TAG)
                    .commit();
        }
    }

    /**
     * Creates the layout for this activity. The default implementation is an empty activity where
     * the fragment consumes the entire window.
     * @see #getFragmentContainerId() To specify the view that your fragment should be attached in.
     */
    protected void onCreateLayout(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_single_fragment);
    }

    /**
     * @return The layout ID that the fragment for this activity should be attached to.
     */
    @IdRes
    protected int getFragmentContainerId() {
        return R.id.fragment_container;
    }

}
