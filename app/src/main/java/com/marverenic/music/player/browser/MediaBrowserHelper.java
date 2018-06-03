package com.marverenic.music.player.browser;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.media.MediaBrowserCompat.MediaItem;
import android.support.v4.media.MediaBrowserServiceCompat.BrowserRoot;
import android.support.v4.media.MediaDescriptionCompat;

import com.marverenic.music.R;
import com.marverenic.music.data.store.MediaStoreUtil;
import com.marverenic.music.data.store.MusicStore;
import com.marverenic.music.data.store.PlaylistStore;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import rx.Single;
import timber.log.Timber;

public class MediaBrowserHelper {

    public static final String MEDIA_ID_ROOT = "__ROOT__";
    public static final String MEDIA_ID_PLAYLIST_ROOT = "__PLAYLIST_ROOT__";
    public static final String MEDIA_ID_EMPTY_ROOT = "__EMPTY_ROOT__";

    protected static final String PLAYLIST_SUBDIR_PREFIX = "playlist/";
    protected static final String QUEUE_SUBDIR_PREFIX = "queue/";

    private Context mContext;

    public MediaBrowserHelper(Context context) {
        mContext = context;
    }

    public BrowserRoot getRoot(boolean empty) {
        return new BrowserRoot((empty) ? MEDIA_ID_EMPTY_ROOT : MEDIA_ID_PLAYLIST_ROOT, null);
    }

    public Single<List<MediaItem>> getContents(String parentId, PlayerController playerController,
                                               MusicStore musicStore, PlaylistStore playlistStore) {
        switch (parentId) {
            case MEDIA_ID_EMPTY_ROOT:
                return Single.just(new ArrayList<>());
            case MEDIA_ID_ROOT:
                return getRootContents();
            case MEDIA_ID_PLAYLIST_ROOT:
                return getPlaylistRootContents(playlistStore);
        }

        return Single.error(new IllegalArgumentException("Path \"" + parentId + "\" does not exist"));
    }

    private Single<List<MediaItem>> getRootContents() {
        return Single.just(Arrays.asList(
                generateMediaItemDirectory(R.string.header_playlists, MEDIA_ID_PLAYLIST_ROOT)
        ));
    }

    private Single<List<MediaItem>> getPlaylistRootContents(PlaylistStore playlistStore) {
        return playlistStore.getPlaylists()
                .first()
                .toSingle()
                .map(playlists -> {
                    List<MediaItem> mediaItems = new ArrayList<>();
                    for (Playlist playlist : playlists) {
                        mediaItems.add(new MediaItem(
                                new MediaDescriptionCompat.Builder()
                                        .setMediaId(PLAYLIST_SUBDIR_PREFIX + playlist.getPlaylistId())
                                        .setTitle(playlist.getPlaylistName())
                                        .build(),
                                MediaItem.FLAG_PLAYABLE
                        ));
                    }

                    return mediaItems;
                });
    }

    @Nullable
    public MediaList decode(String mediaId) {
        if (mediaId.startsWith(PLAYLIST_SUBDIR_PREFIX)) {
            List<Song> songs = loadPlaylistContents(mediaId);
            if (songs != null) {
                return new MediaList(songs, (int) (Math.random() * songs.size()), true);
            }
        } else if (mediaId.startsWith(QUEUE_SUBDIR_PREFIX)) {
            int startIndex;
            try {
                startIndex = Integer.parseInt(mediaId.substring(QUEUE_SUBDIR_PREFIX.length()));
            } catch (NumberFormatException e) {
                Timber.e(e, "Invalid queue position: \"%s\"", mediaId);
                return null;
            }

            return new MediaList(startIndex, false);
        }

        Timber.e("Path \"%s\" does not contain any music", mediaId);
        return null;
    }

    private List<Song> loadPlaylistContents(String mediaId) {
        long playlistId;
        try {
            playlistId = Long.parseLong(mediaId.substring(PLAYLIST_SUBDIR_PREFIX.length()));
        } catch (NumberFormatException e) {
            Timber.e(e, "Invalid playlist ID: \"%s\"", mediaId);
            return null;
        }

        return MediaStoreUtil.getPlaylistSongs(mContext, playlistId);
    }

    private MediaItem generateMediaItemDirectory(@StringRes int name, String id) {
        return new MediaItem(
                new MediaDescriptionCompat.Builder()
                        .setMediaId(id)
                        .setTitle(mContext.getString(name))
                        .build(),
                MediaItem.FLAG_BROWSABLE);
    }

    private List<MediaItem> convertToMediaItem(List<Song> songs, SongIdExtractor idExtractor) {
        List<MediaItem> mediaItems = new ArrayList<>();
        for (int i = 0; i < songs.size(); i++) {
            Song song = songs.get(i);
            mediaItems.add(new MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(idExtractor.getId(song, i))
                            .setTitle(song.getSongName())
                            .setSubtitle(song.getArtistName())
                            .setDescription(song.getAlbumName())
                            .setMediaUri(song.getLocation())
                            .build(),
                    MediaItem.FLAG_PLAYABLE));
        }
        return mediaItems;
    }

    private interface SongIdExtractor {
        String getId(Song song, int idx);
    }

    public static class MediaList {

        public final List<Song> songs;
        public final int startIndex;
        public final boolean shuffle;
        public final boolean keepCurrentQueue;

        private MediaList(List<Song> songs, int startingPosition, boolean shuffle) {
            this.songs = songs;
            this.startIndex = startingPosition;
            this.shuffle = shuffle;
            keepCurrentQueue = false;
        }

        private MediaList(int startIndex, boolean shuffle) {
            this.songs = null;
            this.startIndex = startIndex;
            this.shuffle = shuffle;
            this.keepCurrentQueue = true;
        }

    }

}
