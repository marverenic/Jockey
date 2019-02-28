package com.marverenic.music.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MusicUtils {

    private MusicUtils() {
        throw new RuntimeException("MusicUtils should not be instantiated");
    }

    /**
     * This function implements a shuffling strategy specific to music. It is exposed to allow
     * shuffled song lists to be computed in a consistent way. You may choose to call this to
     * determine what the shuffled queue will be without having to leave the boundaries of your
     * process.
     *
     * @param list The list of tracks to be shuffled.
     * @param startingIndex The song that is either currently played or should be the start of
     *                      playback for this tracklist. This song will always be placed at the
     *                      beginning of the queue.
     * @param seed A random seed used to shuffle the list.
     * @param <T> The type contained in {@param list} that will be shuffled.
     * @return A randomly shuffled list, where the item in {@param list} at index
     * {@param startingIndex} will always be in index 0 of the returned list.
     */
    public static <T> List<T> generateShuffledQueue(List<T> list, int startingIndex, long seed) {
        List<T> shuffled = new ArrayList<>(list);

        if (!shuffled.isEmpty()) {
            T first = shuffled.remove(startingIndex);

            Collections.shuffle(shuffled, new Random(seed));
            shuffled.add(0, first);
        }

        return shuffled;
    }

}
