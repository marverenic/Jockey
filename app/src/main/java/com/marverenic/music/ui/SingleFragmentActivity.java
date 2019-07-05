package com.marverenic.music.ui;

import android.os.Bundle;
import androidx.annotation.IdRes;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import android.view.ViewGroup;

import com.marverenic.music.R;
import com.marverenic.music.view.ViewUtils;

import java.util.List;

public abstract class SingleFragmentActivity extends BaseActivity {

    private static final String CONTENT_FRAGMENT_TAG = "content_fragment";

    protected abstract Fragment onCreateFragment(Bundle savedInstanceState);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        onCreateLayout(savedInstanceState);

        if (getSupportFragmentManager().findFragmentByTag(CONTENT_FRAGMENT_TAG) == null) {
            replaceFragment(onCreateFragment(savedInstanceState));
        }
    }

    /**
     * Used to replace the current content fragment specified initially by
     * {@link #onCreateFragment(Bundle)}. In addition to replacing the current fragment, it will
     * also ensure that all children of the current content fragment are removed from the view.
     * @param replacement The new fragment to be shown in {@link #getFragmentContainerId()}
     */
    protected void replaceFragment(Fragment replacement) {
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();

        for (Fragment fragment : fragments) {
            // Make sure that any child fragments are also removed
            if (isFragmentPartOfContent(fragment)) {
                transaction = transaction.remove(fragment);
            }
        }

        transaction.add(getFragmentContainerId(), replacement, CONTENT_FRAGMENT_TAG).commit();
    }

    /**
     * Determines whether a particular fragment belongs to the content of this activity, or if it's
     * a complementary fragment such as the player control toolbar.
     * @param fragment The fragment to be checked
     * @return {@code true} if the fragment is a direct or indirect child of the fragment container
     * view (specified by {@link #getFragmentContainerId()}).
     */
    private boolean isFragmentPartOfContent(Fragment fragment) {
        ViewGroup fragmentContainer = findViewById(getFragmentContainerId());
        return ViewUtils.viewGroupContains(fragmentContainer, fragment.getView());
    }

    /**
     * @return The fragment created in {@link #onCreateFragment(Bundle)}, if it is still attached
     * to this activity.
     */
    protected Fragment getContentFragment() {
        getSupportFragmentManager().executePendingTransactions();
        return getSupportFragmentManager().findFragmentByTag(CONTENT_FRAGMENT_TAG);
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
