package com.marverenic.music.ui.search;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.view.MenuItem;

import com.marverenic.music.JockeyApplication;
import com.marverenic.music.R;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Album;
import com.marverenic.music.model.Genre;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.marverenic.music.ui.BaseLibraryActivity;
import com.marverenic.music.ui.library.LibraryActivity;

import java.util.ArrayList;

import javax.inject.Inject;

import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import timber.log.Timber;

public class SearchActivity extends BaseLibraryActivity {

    @Inject MusicStore mMusicStore;
    @Inject PlaylistStore mPlaylistStore;
    @Inject PlayerController mPlayerController;

    public static Intent newIntent(Context context) {
        return new Intent(context, SearchActivity.class);
    }

    @Override
    protected Fragment onCreateFragment(Bundle savedInstanceState) {
        return SearchFragment.newInstance();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        JockeyApplication.getComponent(this).inject(this);

        handleIntent(getIntent());
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
                    playArtistResults(query);
                } else if (MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE.equals(focus)) {
                    playAlbumResults(query);
                } else if (focus.equals(MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE)) {
                    playGenreResults(query);
                } else {
                    playSongResults(query);
                }
            }
        }
    }

    private void setSearchQuery(String query) {
        Fragment fragment = getContentFragment();
        if (fragment instanceof SearchFragment) {
            SearchFragment searchFragment = (SearchFragment) fragment;
            searchFragment.setSearchQuery(query);
        } else {
            Timber.w("Attached fragment is not an instance of SearchFragment");
        }
    }

    private void playSongResults(String query) {
        mMusicStore.searchForSongs(query)
                .first()
                .filter(songs -> !songs.isEmpty())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to start playback from query");
                });
    }

    private void playPlaylistResults(String query) {
        mPlaylistStore.searchForPlaylists(query)
                .first()
                .filter(playlists -> !playlists.isEmpty())
                .map(playlists -> {
                    // Find a playlist with a matching name, or default to the first playlist
                    for (Playlist playlist : playlists) {
                        if (playlist.getPlaylistName().equalsIgnoreCase(query)) {
                            return playlist;
                        }
                    }

                    return playlists.get(0);
                })
                .flatMap(playlist -> mPlaylistStore.getSongs(playlist).first())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to start playback from query");
                });
    }

    private void playArtistResults(String query) {
        // If one or more artists with this name exist, play songs by all of them (Ideally this only
        // includes collaborating artists and keeps the search relevant)
        mMusicStore.searchForArtists(query)
                .first()
                .filter(artists -> !artists.isEmpty())
                .flatMap(Observable::from)
                .flatMap(artist -> mMusicStore.getSongs(artist).first())
                .reduce(new ArrayList<Song>(), (aggregate, songs) -> {
                    aggregate.addAll(songs);
                    return aggregate;
                })
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to start playback from query");
                });
    }

    private void playAlbumResults(String query) {
        mMusicStore.searchForAlbums(query)
                .first()
                .filter(albums -> !albums.isEmpty())
                .map(albums -> {
                    // Find an album with a matching name, or default to the first genre
                    for (Album album : albums) {
                        if (album.getAlbumName().equalsIgnoreCase(query)) {
                            return album;
                        }
                    }

                    return albums.get(0);
                })
                .flatMap(album -> mMusicStore.getSongs(album).first())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to start playback from query");
                });
    }

    private void playGenreResults(String query) {
        mMusicStore.searchForGenres(query)
                .first()
                .filter(genres -> !genres.isEmpty())
                .map(genres -> {
                    // Find a genre with a matching name, or default to the first genre
                    for (Genre genre : genres) {
                        if (genre.getGenreName().equalsIgnoreCase(query)) {
                            return genre;
                        }
                    }

                    return genres.get(0);
                })
                .flatMap(genre -> mMusicStore.getSongs(genre).first())
                .compose(bindToLifecycle())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(songs -> {
                    mPlayerController.setQueue(songs, 0);
                    mPlayerController.play();
                }, throwable -> {
                    Timber.e(throwable, "Failed to start playback from query");
                });
    }

}
