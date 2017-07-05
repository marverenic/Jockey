package com.marverenic.music.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import com.marverenic.music.R;

public abstract class SingleFragmentActivity extends BaseActivity {

    protected abstract Fragment onCreateFragment(Bundle savedInstanceState);

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_fragment);

        Fragment fragment = onCreateFragment(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, fragment)
                .commit();
    }
}
