package com.marverenic.music.viewmodel;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.RobolectricJockeyApplication;
import com.marverenic.music.activity.instance.AutoPlaylistActivity;
import com.marverenic.music.activity.instance.PlaylistActivity;
import com.marverenic.music.model.AutoPlaylist;
import com.marverenic.music.model.Playlist;
import com.marverenic.music.model.playlistrules.AutoPlaylistRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;

import static com.marverenic.music.utils.AssertUtils.assertBundlesEqual;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 23, constants = BuildConfig.class, application = RobolectricJockeyApplication.class)
public class PlaylistViewModelTest {

    private FragmentActivity mActivity;
    private Playlist mModel;
    private AutoPlaylist mAutoModel;
    private PlaylistViewModel mSubject;

    @Before
    public void setup() {
        mActivity = Robolectric.buildActivity(FragmentActivity.class)
                .create()
                .start()
                .resume()
                .get();

        mModel = new Playlist.Builder()
                .setPlaylistId(5)
                .setPlaylistName("A Playlist")
                .build();

        mAutoModel = new AutoPlaylist.Builder()
                .setName("A smart playlist")
                .setId(4)
                .setMatchAllRules(true)
                .setMaximumEntries(25)
                .setRules(
                        new AutoPlaylistRule.Factory()
                                .setType(AutoPlaylistRule.SONG)
                                .setField(AutoPlaylistRule.PLAY_COUNT)
                                .setMatch(AutoPlaylistRule.GREATER_THAN)
                                .setValue("0")
                                .build())
                .setSortAscending(true)
                .setSortMethod(AutoPlaylistRule.PLAY_COUNT)
                .build();

        mSubject = new PlaylistViewModel(mActivity);
    }

    @Test
    public void testCorrectText() {
        mSubject.setPlaylist(mModel);
        assertEquals("A Playlist", mSubject.getName());

        mSubject.setPlaylist(mAutoModel);
        assertEquals("A smart playlist", mSubject.getName());
    }

    @Test
    public void testAutoPlaylistShowsIndicator() {
        mSubject.setPlaylist(mAutoModel);
        assertEquals(View.VISIBLE, mSubject.getSmartIndicatorVisibility());
    }

    @Test
    public void testPlaylistHidesIndicator() {
        mSubject.setPlaylist(mModel);
        assertEquals(View.GONE, mSubject.getSmartIndicatorVisibility());
    }

    @Test
    public void testPlaylistClick() {
        mSubject.setPlaylist(mModel);
        mSubject.onClickPlaylist().onClick(null);

        Intent expected = PlaylistActivity.newIntent(mActivity, mModel);
        Intent actual = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertTrue(expected.filterEquals(actual));
        assertBundlesEqual(expected.getExtras(), actual.getExtras());
    }

    @Test
    public void testAutoPlaylistClick() {
        mSubject.setPlaylist(mAutoModel);
        mSubject.onClickPlaylist().onClick(null);

        Intent expected = AutoPlaylistActivity.newIntent(mActivity, mAutoModel);
        Intent actual = Shadows.shadowOf(mActivity).getNextStartedActivity();
        assertTrue(expected.filterEquals(actual));
        assertBundlesEqual(expected.getExtras(), actual.getExtras());
    }

}
