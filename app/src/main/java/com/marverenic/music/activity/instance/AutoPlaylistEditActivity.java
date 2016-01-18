package com.marverenic.music.activity.instance;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.marverenic.music.R;
import com.marverenic.music.activity.BaseActivity;
import com.marverenic.music.instances.AutoPlaylist;
import com.marverenic.music.instances.Library;
import com.marverenic.music.instances.section.RuleHeaderSingleton;
import com.marverenic.music.instances.section.RuleSection;
import com.marverenic.music.utils.Navigate;
import com.marverenic.music.utils.Themes;
import com.marverenic.music.view.BackgroundDecoration;
import com.marverenic.music.view.DividerDecoration;
import com.marverenic.music.view.EnhancedAdapters.HeterogeneousAdapter;

import java.util.ArrayList;
import java.util.Collections;

public class AutoPlaylistEditActivity extends BaseActivity
        implements RuleSection.OnRemovalListener {

    public static final String PLAYLIST_EXTRA = "auto-playlist";
    private static final String EDITED_HEADER = "modified-header";
    private static final String EDITED_RULES = "modified-rules";

    private AutoPlaylist reference;
    private AutoPlaylist editedReference;
    private ArrayList<AutoPlaylist.Rule> editedRules;
    private HeterogeneousAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_instance_no_miniplayer);

        reference = getIntent().getParcelableExtra(PLAYLIST_EXTRA);
        if (reference == null) {
            reference = AutoPlaylist.EMPTY;
        }

        if (savedInstanceState != null) {
            editedReference = savedInstanceState.getParcelable(EDITED_HEADER);
            editedRules = savedInstanceState.getParcelableArrayList(EDITED_RULES);
        }

        if (editedReference == null || editedRules == null) {
            editedReference = new AutoPlaylist(reference);
            editedRules = new ArrayList<>(reference.getRules().length);
            Collections.addAll(editedRules, editedReference.getRules());
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
                .addSection(new RuleHeaderSingleton(editedReference))
                .addSection(new RuleSection(editedRules, this));

        RecyclerView list = (RecyclerView) findViewById(R.id.list);
        list.setAdapter(adapter);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.addItemDecoration(new BackgroundDecoration(Themes.getBackgroundElevated()));
        list.addItemDecoration(new DividerDecoration(this));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_auto_playlist_editor, menu);
        return true;
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(EDITED_HEADER, editedReference);
        outState.putParcelableArrayList(EDITED_RULES, editedRules);
    }

    public void saveChanges() {
        AutoPlaylist.Rule[] modifiedRules = new AutoPlaylist.Rule[editedRules.size()];
        editedReference.setRules(editedRules.toArray(modifiedRules));
        if (reference.getPlaylistId() == AutoPlaylist.EMPTY.getPlaylistId()) {
            Library.createAutoPlaylist(this, editedReference);
        } else {
            Library.editAutoPlaylist(this, editedReference);
        }
    }

    private boolean rulesChanged() {
        if (editedRules.size() != reference.getRules().length) {
            return true;
        }

        for (int i = 0; i < editedRules.size(); i++) {
            if (!reference.getRules()[i].equals(editedRules.get(i))) {
                return true;
            }
        }

        return false;
    }

    private boolean validateName() {
        boolean valid =
                editedReference.getPlaylistName().trim().equalsIgnoreCase(
                        reference.getPlaylistName().trim())
                || Library.verifyPlaylistName(this, editedReference.getPlaylistName()) == null;

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
                editedRules.add(new AutoPlaylist.Rule(AutoPlaylist.Rule.EMPTY));
                adapter.notifyItemInserted(editedRules.size());
                return true;
            case R.id.discard:
                if (rulesChanged()) {
                    AlertDialog dialog = new AlertDialog.Builder(this)
                            .setMessage("Discard changes?")
                            .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    finish();
                                }
                            })
                            .setNegativeButton(R.string.action_cancel, null)
                            .show();

                    Themes.themeAlertDialog(dialog);
                } else {
                    finish();
                }
                return true;
            case android.R.id.home:
                if (validateName()) {
                    if (!editedReference.isEqual(reference) || rulesChanged()) {
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
        if (!editedReference.isEqual(reference) || rulesChanged()) {
            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setMessage("Save changes?")
                    .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (validateName()) {
                                saveChanges();
                                Navigate.back(AutoPlaylistEditActivity.this);
                            }
                        }
                    })
                    .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Navigate.back(AutoPlaylistEditActivity.this);
                        }
                    })
                    .setNeutralButton("Cancel", null)
                    .show();

            Themes.themeAlertDialog(dialog);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void updateMiniplayer() {

    }

    @Override
    public void onRuleRemoved(final AutoPlaylist.Rule rule, final int index) {
        editedRules.remove(index - 1);
        adapter.notifyItemRemoved(index);

        Snackbar.make(findViewById(R.id.list), "Removed rule", Snackbar.LENGTH_LONG)
                .setAction(R.string.action_undo, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editedRules.add(index - 1, rule);
                        adapter.notifyItemInserted(index);
                    }
                })
                .show();
    }
}
