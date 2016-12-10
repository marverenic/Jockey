package com.marverenic.music.utils;

import android.os.Bundle;

import org.junit.Assert;

public class AssertUtils {

    public static void assertBundlesEqual(Bundle expected, Bundle actual) {
        Assert.assertEquals(expected.keySet(), actual.keySet());

        for (String key : expected.keySet()) {
            Assert.assertEquals(expected.get(key), actual.get(key));
        }
    }

}
