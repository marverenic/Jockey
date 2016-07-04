package com.marverenic.music.activity.instance;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
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
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;

import javax.inject.Inject;

public class AutoPlaylistEditActivity extends BaseActivity
        implements RuleSection.OnRemovalListener {

    @Inject PlaylistStore mPlaylistStore;

    public static final String PLAYLIST_EXTRA = "auto-playlist";
    private static final String SAVED_BUILDER = "playlist-builder";

    private AutoPlaylist reference;
    private AutoPlaylist.Builder mBuilder;
    private HeterogeneousAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_no_miniplayer);

        JockeyApplication.getComponent(this).inject(this);

        reference = getIntent().getParcelableExtra(PLAYLIST_EXTRA);
        if (reference == null) {
            reference = emptyPlaylist();
        }

        if (savedInstanceState != null) {
            mBuilder = savedInstanceState.getParcelable(SAVED_BUILDER);
        }

        if (mBuilder == null) {
            mBuilder = new AutoPlaylist.Builder(reference);
        }

        if (getSupportActionBar() != null) {
            if (reference == null) {
                getSupportActionBar().setTitle(R.string.playlist_auto_new);
            } else {
                getSupportActionBar().setTitle(reference.getPlaylistName());
            }
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_done_24dp);
        }

        adapter = new HeterogeneousAdapter()
                .addSection(new RuleHeaderSingleton(mBuilder))
                .addSection(new RuleSection(mBuilder.getRules(), this));

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new BackgroundDecoration());
        list.addItemDecoration(new DividerDecoration(this));
    }

    private static AutoPlaylist emptyPlaylist() {
        return new AutoPlaylist.Builder()
                .setName("")
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
        if (reference.getPlaylistId() == -1) {
            mPlaylistStore.makePlaylist(mBuilder.build());
        } else {
            mPlaylistStore.editPlaylist(mBuilder.build());
        }
    }

    private boolean rulesChanged() {
        if (mBuilder.getRules().size() != reference.getRules().size()) {
            return true;
        }

        for (int i = 0; i < mBuilder.getRules().size(); i++) {
            if (!reference.getRules().get(i).equals(mBuilder.getRules().get(i))) {
                return true;
            }
        }

        return false;
    }

    private boolean validateName() {
        String originalName = reference.getPlaylistName().trim();
        String editedName = mBuilder.getName().trim();

        boolean equal = originalName.equalsIgnoreCase(editedName);
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
                adapter.notifyItemInserted(mBuilder.getRules().size());
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
                    if (!mBuilder.isEqual(reference) || rulesChanged()) {
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
        if (!mBuilder.isEqual(reference) || rulesChanged()) {
            new AlertDialog.Builder(this)
                    .setMessage("Save changes?")
                    .setPositiveButton("Save", (dialog, which) -> {
                        if (validateName()) {
                            saveChanges();
                            Navigate.back(AutoPlaylistEditActivity.this);
                        }
                    })
                    .setNegativeButton("Discard", (dialog, which) -> {
                        Navigate.back(AutoPlaylistEditActivity.this);
                    })
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onRuleRemoved(final AutoPlaylistRule rule, final int index) {
        mBuilder.getRules().remove(index - 1);
        adapter.notifyItemRemoved(index);

        Snackbar.make(findViewById(R.id.list), "Removed rule", Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, v -> {
                    mBuilder.getRules().add(index - 1, rule);
                    adapter.notifyItemInserted(index);
                })
                .show();
    }
}
