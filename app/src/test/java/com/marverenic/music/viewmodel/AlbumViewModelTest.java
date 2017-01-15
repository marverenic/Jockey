package com.marverenic.music.viewmodel;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.RobolectricJockeyApplication;
import com.marverenic.music.activity.instance.AlbumActivity;
import com.marverenic.music.model.Album;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static com.marverenic.music.utils.AssertUtils.assertBundlesEqual;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, constants = BuildConfig.class, application = RobolectricJockeyApplication.class)
public class AlbumViewModelTest {

    private FragmentActivity mActivity;
    private AlbumViewModel mSubject;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        mSubject = new AlbumViewModel(mActivity, mActivity.getSupportFragmentManager());
    }

    @Test
    public void testCorrectText() {
        Album model = new Album.Builder()
                .setAlbumName("Album name")
                .setArtistName("Artist name")
                .build();

        mSubject.setAlbum(model);

        assertEquals("Album name", mSubject.getAlbumTitle());
        assertEquals("Artist name", mSubject.getAlbumArtist());
    }

    @Test
    public void testClickNavigatesToAlbum() {
        Album model = new Album.Builder()
                .setAlbumName("Album")
                .setArtistName("Artist")
                .setAlbumId(10)
                .setArtistId(11)
                .setYear(2016)
                .setArtUri(null)
                .build();

        mSubject.setAlbum(model);
        mSubject.onClickAlbum().onClick(null);

        Intent expected = AlbumActivity.newIntent(mActivity, model);
        Intent actual = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertTrue(expected.filterEquals(actual));
        assertBundlesEqual(expected.getExtras(), actual.getExtras());
    }

}
