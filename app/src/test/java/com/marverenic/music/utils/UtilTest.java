package com.marverenic.music.utils;

import android.webkit.MimeTypeMap;

import com.marverenic.music.BuildConfig;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class)
public class UtilTest {

    @Before
    public void setUp() {
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("mp3", "audio/mpeg");
        shadowOf(MimeTypeMap.getSingleton()).addExtensionMimeTypMapping("txt", "text/plain");
    }

    @After
    public void tearDown() {
        shadowOf(MimeTypeMap.getSingleton()).clearMappings();
    }

    @Test
    public void testIsFileMusic_returnsTrueForMp3() {
        assertTrue(Util.isFileMusic(new File("/sdcard/music/lorem ipsum.mp3")));
    }

    @Test
    public void testIsFileMusic_returnsFalseForOtherMime() {
        assertFalse(Util.isFileMusic(new File("/sdcard/music/lorem ipsum.txt")));
    }

    @Test
    public void testIsFileMusic_returnsFalseForUnknownMime() {
        assertFalse(Util.isFileMusic(new File("/sdcard/music/lorem ipsum.qwerty")));
    }

    @Test
    public void testIsFileMusic_usesLastExtension() {
        assertFalse(Util.isFileMusic(new File("/sdcard/music/lorem ipsum.mp3.txt")));
    }

    @Test
    public void testIsFileMusic_returnsFalseWithNoExtension() {
        assertFalse(Util.isFileMusic(new File("/sdcard/music/lorem ipsum")));
    }

    @Test
    public void testIsFileMusic_returnsFalseForDirectory() {
        assertFalse(Util.isFileMusic(new File("/sdcard/music/")));
    }

    @Test
    public void testIsFileMusic_returnsFalseForHiddenFile() {
        assertFalse(Util.isFileMusic(new File("/sdcard/music/.mp3")));
    }

}
