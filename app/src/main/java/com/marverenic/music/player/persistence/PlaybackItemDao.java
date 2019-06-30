package com.marverenic.music.player.persistence;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PlaybackItemDao {

    @Query("SELECT COUNT(*) FROM queue_items")
    int getPlaybackItemCount();

    @Query("SELECT COUNT(*) FROM playback_metadata")
    int getMetadataItemCount();

    @Query("SELECT * FROM queue_items where list_name IS :listName ORDER BY idx ASC")
    List<PlaybackItem> getPlaybackItems(String listName);

    @Query("SELECT * FROM queue_items where list_name is :listName AND idx is :index LIMIT 1")
    PlaybackItem getPlaybackItemAtIndex(String listName, int index);

    @Query("DELETE FROM queue_items where list_name IS :listName")
    void clearPlaybackItems(String listName);

    @Insert
    void setPlaybackItems(List<PlaybackItem> playbackItems);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void putMetadataItem(PlaybackMetadataItem item);

    @Query("DELETE FROM playback_metadata where `key` is :key")
    void deleteMetadataItem(String key);

    @Query("SELECT * FROM playback_metadata WHERE `key` IS :key LIMIT 1")
    PlaybackMetadataItem getMetadataItem(String key);

}
