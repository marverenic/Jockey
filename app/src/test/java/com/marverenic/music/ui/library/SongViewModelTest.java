package com.marverenic.music.ui.library;

import android.view.View;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.RobolectricJockeyApplication;
import com.marverenic.music.model.Song;
import com.marverenic.music.player.PlayerController;
import com.trello.rxlifecycle.components.support.RxFragmentActivity;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertSame;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, constants = BuildConfig.class, application = RobolectricJockeyApplication.class)
public class SongViewModelTest {

    private RxFragmentActivity mActivity;
    private Song mModel;
    private Song mOtherModel;
    private List<Song> mSurroundingContents;
    private SongViewModel mSubject;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(RxFragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        mSurroundingContents = new ArrayList<>();
        mModel = new Song.Builder()
                .setSongName("Song")
                .setSongId(1)
                .setArtistName("Artist")
                .setArtistId(5)
                .setAlbumName("Album")
                .setAlbumId(10)
                .setSongDuration(TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES))
                .setYear(2016)
                .setDateAdded(System.currentTimeMillis())
                .setTrackNumber(1)
                .setInLibrary(true)
                .build();

        mOtherModel = new Song.Builder()
                .setSongName("Another Song")
                .setSongId(2)
                .setArtistName("Another Artist")
                .setArtistId(6)
                .setAlbumName("Another Album")
                .setAlbumId(11)
                .setSongDuration(TimeUnit.MILLISECONDS.convert(3, TimeUnit.MINUTES))
                .setYear(2017)
                .setDateAdded(System.currentTimeMillis())
                .setTrackNumber(6)
                .setInLibrary(true)
                .build();

        mSubject = new SongViewModel(mActivity, mActivity.getSupportFragmentManager(),
                mActivity.bindToLifecycle(), mSurroundingContents);
    }

    @Test
    public void testCorrectText() {
        mSurroundingContents.add(mModel);
        mSubject.setIndex(0);

        assertEquals("Song", mSubject.getTitle());
        assertEquals("Artist – Album", mSubject.getDetail());
    }

    @Test
    public void testReferenceInSingletonList() {
        mSurroundingContents.add(mModel);
        mSubject.setIndex(0);

        assertEquals(0, mSubject.getIndex());
        assertSame(mSurroundingContents, mSubject.getSongs());
        assertEquals(mModel, mSubject.getReference());
    }

    @Test
    public void testReferenceInGeneralList() {
        mSurroundingContents.add(null);
        mSurroundingContents.add(null);
        mSurroundingContents.add(null);
        mSurroundingContents.add(mModel);
        mSurroundingContents.add(null);
        mSubject.setIndex(3);

        assertEquals(3, mSubject.getIndex());
        assertSame(mSurroundingContents, mSubject.getSongs());
        assertEquals(mModel, mSubject.getReference());
    }

    @Test
    public void testNowPlayingIndicatorWithEmptyQueue() {
        PlayerController playerController = mSubject.mPlayerController;
        playerController.clearQueue();

        mSurroundingContents.add(null);
        mSurroundingContents.add(null);
        mSurroundingContents.add(null);
        mSurroundingContents.add(mModel);
        mSurroundingContents.add(null);
        mSubject.setIndex(3);

        assertEquals(View.GONE, mSubject.getNowPlayingIndicatorVisibility());
    }

    @Test
    public void testNowPlayingIndicatorWithGeneralQueue() {
        PlayerController playerController = mSubject.mPlayerController;
        playerController.clearQueue();

        mSurroundingContents.add(mOtherModel);
        mSurroundingContents.add(mOtherModel);
        mSurroundingContents.add(mOtherModel);
        mSurroundingContents.add(mModel);
        mSurroundingContents.add(mOtherModel);
        mSubject.setIndex(3);

        playerController.setQueue(mSurroundingContents, 3);
        assertEquals(View.VISIBLE, mSubject.getNowPlayingIndicatorVisibility());

        playerController.changeSong(2);
        assertEquals(View.GONE, mSubject.getNowPlayingIndicatorVisibility());
    }

}
