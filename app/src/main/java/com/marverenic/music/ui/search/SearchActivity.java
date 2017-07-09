package com.marverenic.music.ui.search;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.ui.library.LibraryActivity;

import javax.inject.Inject;

public class SearchActivity extends BaseLibraryActivity {


    public static Intent newIntent(Context context) {
        return new Intent(context, SearchActivity.class);
    }

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayerController mPlayerController;

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        return SearchFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

/*
        handleIntent(getIntent());
*/
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_library_search:
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) onSearchRequested();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        Intent mainActivity = new Intent(this, LibraryActivity.class);
        mainActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivity(mainActivity);
        return true;
    }
/*
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null) {
            // Handle standard searches
            if (Intent.ACTION_SEARCH.equals(intent.getAction())
                    || MediaStore.INTENT_ACTION_MEDIA_SEARCH.equals(intent.getAction())) {
                setSearchQuery(intent.getStringExtra(SearchManager.QUERY));

            } else if (MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH.equals(intent.getAction())) {
                // Handle play from search actions
                String query = intent.getStringExtra(SearchManager.QUERY);
                String focus = intent.getStringExtra(MediaStore.EXTRA_MEDIA_FOCUS);

                setSearchQuery(query);

                if (MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE.equals(focus)) {
                    playPlaylistResults(query);
                } else if (MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE.equals(focus)) {
                    playArtistResults();
                } else if (MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE.equals(focus)) {
                    playAlbumResults(query);
                } else if (focus.equals(MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)) {
                    playGenreResults(query);
                } else {
                    playSongResults();
                }
            }
        }
    }

    private void setSearchQuery(String query) {
        Fragment fragment = getContentFragment();
        if (fragment instanceof SearchFragment) {
            SearchFragment searchFragment = (SearchFragment) fragment;
            searchFragment.setSearchQuery(query);
        }
    }

    private void playSongResults() {
        if (!getSongResults().isEmpty()) {
            mPlayerController.setQueue(getSongResults(), 0);
            mPlayerController.play();
        }
    }

    private void playPlaylistResults(String query) {
        if (getPlaylistResults().isEmpty()) {
            return;
        }

        // If there is a playlist with this exact name, use it, otherwise fallback
        // to the first result
        Playlist playlist = getPlaylistResults().get(0);
        for (Playlist p : getPlaylistResults()) {
            if (p.getPlaylistName().equalsIgnoreCase(query)) {
                playlist = p;
                break;
            }
        }

        mPlaylistStore.getSongs(playlist).subscribe(
                songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to play playlist from intent");
                });
    }

    private void playArtistResults() {
        if (getGenreResults().isEmpty()) {
            return;
        }

        // If one or more artists with this name exist, play songs by all of them (Ideally this only
        // includes collaborating artists and keeps the search relevant)
        Observable<List<Song>> combinedSongs = Observable.just(new ArrayList<>());
        for (Artist a : getArtistResults()) {
            combinedSongs = Observable.combineLatest(
                    combinedSongs, mMusicStore.getSongs(a), (left, right) -> {
                        left.addAll(right);
                        return left;
                    });
        }

        combinedSongs.subscribe(
                songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                },
                throwable -> {
                    Timber.e(throwable, "Failed to play artist from intent");
                });
    }

    private void playAlbumResults(String query) {
        if (getAlbumResults().isEmpty()) {
            return;
        }

        // If albums with this name exist, look for an exact match
        // If we find one then use it, otherwise fallback to the first result
        Album album = getAlbumResults().get(0);
        for (Album a : getAlbumResults()) {
            if (a.getAlbumName().equalsIgnoreCase(query)) {
                album = a;
                break;
            }
        }

        mMusicStore.getSongs(album).subscribe(
                songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to play album from intent");
                });
    }

    private void playGenreResults(String query) {
        if (!getGenreResults().isEmpty()) {
            return;
        }
        // If genres with this name exist, look for an exact match
        // If we find one then use it, otherwise fallback to the first result
        Genre genre = getGenreResults().get(0);
        for (Genre g : getGenreResults()) {
            if (g.getGenreName().equalsIgnoreCase(query)) {
                genre = g;
                break;
            }
        }

        mMusicStore.getSongs(genre).subscribe(
                songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to play genre from intent");
                });
    }
*/
}
