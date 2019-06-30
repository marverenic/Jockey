package com.marverenic.music.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.marverenic.music.R;
import com.marverenic.music.ui.SingleFragmentActivity;
import com.marverenic.music.utils.Util;

public class EqualizerActivity extends SingleFragmentActivity {

    @Nullable
    public static Intent newIntent(Context context, boolean skipSystemEq) {
        boolean hasSystemEq = Util.getSystemEqIntent(context) != null;
        boolean hasInternalEq = Util.hasEqualizer();

        if (!hasSystemEq && !hasInternalEq) {
            // No equalizers are available
            return null;
        } else if (hasSystemEq && !(skipSystemEq && hasInternalEq)) {
            return Util.getSystemEqIntent(context);
        } else {
            return new Intent(context, EqualizerActivity.class);
        }
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        return EqualizerFragment.newInstance();
    }

    @Override
    protected void onCreateLayout(@Nullable Bundle savedInstanceState) {
        setContentView(R.layout.activity_settings);
        setSupportActionBar(findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    protected int getFragmentContainerId() {
        return R.id.pref_fragment_container;
    }

}
