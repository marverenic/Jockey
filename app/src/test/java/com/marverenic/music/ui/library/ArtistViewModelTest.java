package com.marverenic.music.ui.library;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.RobolectricJockeyApplication;
import com.marverenic.music.ui.library.ArtistViewModel;
import com.marverenic.music.ui.library.artist.ArtistActivity;
import com.marverenic.music.model.Artist;

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
public class ArtistViewModelTest {

    private FragmentActivity mActivity;
    private ArtistViewModel mSubject;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        mSubject = new ArtistViewModel(mActivity, mActivity.getSupportFragmentManager());
    }

    @Test
    public void testCorrectText() {
        Artist model = new Artist.Builder()
                .setArtistName("Artist")
                .setArtistId(10)
                .build();

        mSubject.setArtist(model);
        assertEquals("Artist", mSubject.getName());
    }

    @Test
    public void testClickNavigatesToArtist() {
        Artist model = new Artist.Builder()
                .setArtistName("Artist")
                .setArtistId(10)
                .build();

        mSubject.setArtist(model);
        mSubject.onClickArtist().onClick(null);

        Intent expected = ArtistActivity.newIntent(mActivity, model);
        Intent actual = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertTrue(expected.filterEquals(actual));
        assertBundlesEqual(expected.getExtras(), actual.getExtras());
    }

}
