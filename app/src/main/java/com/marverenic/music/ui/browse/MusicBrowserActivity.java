package com.marverenic.music.ui.browse;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.marverenic.music.R;
import com.marverenic.music.model.Song;
import com.marverenic.music.ui.BaseToolbarFragment;
import com.marverenic.music.ui.SingleFragmentActivity;
import com.marverenic.music.utils.UriUtils;

import java.io.File;

public class MusicBrowserActivity extends SingleFragmentActivity {

    private static final String EXTRA_STARTING_DIRECTORY = "MusicBrowserActivity.StartingDirectory";

    public static Intent newIntent(Context context, Song targetSong) {
        String path = UriUtils.getPathFromUri(context, targetSong.getLocation());
        if (path != null) {
            path = new File(path).getParent();
        }

        Intent intent = new Intent(context, MusicBrowserActivity.class);
        intent.putExtra(EXTRA_STARTING_DIRECTORY, path);
        return intent;
    }

    public static Intent newIntent(Context context, File startingDirectory) {
        Intent intent = new Intent(context, MusicBrowserActivity.class);
        intent.putExtra(EXTRA_STARTING_DIRECTORY, startingDirectory.getAbsolutePath());
        return intent;
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        String startingPath = getIntent().getStringExtra(EXTRA_STARTING_DIRECTORY);
        if (startingPath == null) {
            return UnknownLocationFragment.newInstance();
        } else {
            return MusicBrowserFragment.newInstance(new File(startingPath), false);
        }
    }

    @Override
    public void setSupportActionBar(@Nullable Toolbar toolbar) {
        super.setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class UnknownLocationFragment extends BaseToolbarFragment {

        public static UnknownLocationFragment newInstance() {
            return new UnknownLocationFragment();
        }

        @Override
        protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container,
                                           @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.instance_empty, container, false);
            TextView title = view.findViewById(R.id.empty_message);
            title.setText(R.string.empty_error_folder);
            return view;
        }

        @Override
        protected String getFragmentTitle() {
            return getString(R.string.header_browser);
        }

        @Override
        protected boolean canNavigateUp() {
            return true;
        }
    }

}
