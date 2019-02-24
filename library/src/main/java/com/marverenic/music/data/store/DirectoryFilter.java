package com.marverenic.music.data.store;

import java.util.Set;

public interface DirectoryFilter {

    Set<String> getIncludedDirectories();
    Set<String> getExcludedDirectories();

}
