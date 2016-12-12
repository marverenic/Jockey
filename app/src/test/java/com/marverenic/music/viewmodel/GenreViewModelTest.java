package com.marverenic.music.viewmodel;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.activity.instance.GenreActivity;
import com.marverenic.music.model.Genre;

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
@Config(sdk = 23, constants = BuildConfig.class)
public class GenreViewModelTest {

    private FragmentActivity mActivity;
    private GenreViewModel mSubject;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        mSubject = new GenreViewModel(mActivity, mActivity.getSupportFragmentManager());
    }

    @Test
    public void testCorrectText() {
        Genre model = new Genre.Builder()
                .setGenreName("Genre")
                .setGenreId(10)
                .build();

        mSubject.setGenre(model);
        assertEquals("Genre", mSubject.getName());
    }

    @Test
    public void testClickNavigatesToArtist() {
        Genre model = new Genre.Builder()
                .setGenreName("Genre")
                .setGenreId(10)
                .build();

        mSubject.setGenre(model);
        mSubject.onClickGenre().onClick(null);

        Intent expected = GenreActivity.newIntent(mActivity, model);
        Intent actual = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertTrue(expected.filterEquals(actual));
        assertBundlesEqual(expected.getExtras(), actual.getExtras());
    }


}
