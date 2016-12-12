package com.marverenic.music.viewmodel;

import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.model.Song;

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
@Config(sdk = 23, constants = BuildConfig.class)
public class SongViewModelTest {

    private FragmentActivity mActivity;
    private Song mModel;
    private List<Song> mSurroundingContents;
    private SongViewModel mSubject;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(FragmentActivity.class)
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

        FragmentManager fragmentManager = mActivity.getSupportFragmentManager();
        mSubject = new SongViewModel(mActivity, fragmentManager, mSurroundingContents);
    }

    @Test
    public void testCorrectText() {
        mSurroundingContents.add(mModel);
        mSubject.setIndex(0);

        assertEquals("Song", mSubject.getTitle());
        assertEquals("Artist - Album", mSubject.getDetail());
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

}
