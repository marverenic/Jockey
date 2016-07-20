package com.marverenic.music.activity.instance;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;

import com.marverenic.heterogeneousadapter.HeterogeneousAdapter;
import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.playlistrules.AutoPlaylistRule;
import com.marverenic.music.instances.section.RuleHeaderSingleton;
import com.marverenic.music.instances.section.RuleSection;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import javax.inject.Inject;

public class AutoPlaylistEditActivity extends BaseActivity {

    private static final String PLAYLIST_EXTRA = "AutoPlaylistEditActivity.PLAYLIST";
    private static final String SAVED_BUILDER = "AutoPlaylistEditActivity.PLAYLIST_BUILDER";

    @Inject PlaylistStore mPlaylistStore;

    private AutoPlaylist mOriginalPlaylist;
    private AutoPlaylist.Builder mBuilder;
    private HeterogeneousAdapter mAdapter;

    public static Intent newIntent(Context context) {
        return newIntent(context, null);
    }

    public static Intent newIntent(Context context, AutoPlaylist target) {
        Intent intent = new Intent(context, AutoPlaylistEditActivity.class);
        intent.putExtra(PLAYLIST_EXTRA, target);

        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_no_miniplayer);

        JockeyApplication.getComponent(this).inject(this);

        mOriginalPlaylist = getIntent().getParcelableExtra(PLAYLIST_EXTRA);
        if (mOriginalPlaylist == null) {
            mOriginalPlaylist = emptyPlaylist();
        }

        if (savedInstanceState != null) {
            mBuilder = savedInstanceState.getParcelable(SAVED_BUILDER);
        }

        if (mBuilder == null) {
            mBuilder = new AutoPlaylist.Builder(mOriginalPlaylist);
        }

        if (getSupportActionBar() != null) {
            if (mOriginalPlaylist == null) {
                getSupportActionBar().setTitle(R.string.playlist_auto_new);
            } else {
                getSupportActionBar().setTitle(mOriginalPlaylist.getPlaylistName());
            }
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_done_24dp);
        }

        mAdapter = new HeterogeneousAdapter()
                .addSection(new RuleHeaderSingleton(mOriginalPlaylist, mBuilder))
                .addSection(new RuleSection(mBuilder.getRules()));

        mAdapter.setHasStableIds(true);

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(mAdapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new BackgroundDecoration());
        list.addItemDecoration(new DividerDecoration(this));
    }

    private static AutoPlaylist emptyPlaylist() {
        return new AutoPlaylist.Builder()
                .setName("")
                .setId(AutoPlaylist.Builder.NO_ID)
                .setMatchAllRules(true)
                .setMaximumEntries(-25)
                .setSortMethod(AutoPlaylistRule.ID)
                .setTruncateMethod(AutoPlaylistRule.ID)
                .setTruncateAscending(true)
                .setRules(emptyRule())
                .build();
    }

    private static AutoPlaylistRule emptyRule() {
        return new AutoPlaylistRule.Factory()
                .setType(AutoPlaylistRule.SONG)
                .setField(AutoPlaylistRule.NAME)
                .setMatch(AutoPlaylistRule.CONTAINS)
                .setValue("")
                .build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_auto_playlist_editor, menu);
        return true;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(SAVED_BUILDER, mBuilder);
    }

    public void saveChanges() {
        if (mOriginalPlaylist.getPlaylistId() == AutoPlaylist.Builder.NO_ID) {
            mPlaylistStore.makePlaylist(mBuilder.build());
        } else {
            mPlaylistStore.editPlaylist(mBuilder.build());
        }
    }

    private boolean rulesChanged() {
        if (mBuilder.getRules().size() != mOriginalPlaylist.getRules().size()) {
            return true;
        }

        for (int i = 0; i < mBuilder.getRules().size(); i++) {
            if (!mOriginalPlaylist.getRules().get(i).equals(mBuilder.getRules().get(i))) {
                return true;
            }
        }

        return false;
    }

    private boolean validateName() {
        String originalName = mOriginalPlaylist.getPlaylistName().trim();
        String editedName = mBuilder.getName().trim();

        boolean equal = !mOriginalPlaylist.getPlaylistName().trim().isEmpty()
                && originalName.equalsIgnoreCase(editedName);

        boolean valid = equal || mPlaylistStore.verifyPlaylistName(editedName) == null;

        if (!valid) {
            RecyclerView list = (RecyclerView) findViewById(R.id.list);
            list.scrollToPosition(0);
        }
        return valid;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.add:
                mBuilder.getRules().add(emptyRule());
                mAdapter.notifyItemInserted(mBuilder.getRules().size());
                return true;
            case R.id.discard:
                if (rulesChanged()) {
                    new AlertDialog.Builder(this)
                            .setMessage("Discard changes?")
                            .setPositiveButton("Discard", (dialog, which) -> {finish();})
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();
                } else {
                    finish();
                }
                return true;
            case android.R.id.home:
                if (validateName()) {
                    if (!mBuilder.isEqual(mOriginalPlaylist) || rulesChanged()) {
                        saveChanges();
                    }
                } else {
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (!mBuilder.isEqual(mOriginalPlaylist) || rulesChanged()) {
            new AlertDialog.Builder(this)
                    .setMessage("Save changes?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        if (validateName()) {
                            saveChanges();
                            super.onBackPressed();
                        }
                    })
                    .setNegativeButton("Discard", (dialog, which) -> {
                        super.onBackPressed();
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
