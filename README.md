# Jockey
Jockey is a music player for Android based on Google's Material Design standards. The point of Jockey is to be a simple, lightweight media player with features designed for music enthusiasts and casual listeners alike. Currently Jockey is notably lacking a few enthusiast features like gapless playback, crossfade and replay gain which may be added in the future.

[![Get it on Google Play](https://github.com/marverenic/Jockey/raw/master/screenshots/play-badge.png)](https://play.google.com/store/apps/details?id=com.marverenic.music)  

<img height="400px" src="https://github.com/marverenic/Jockey/raw/master/screenshots/NowPlaying5_framed.png">
<img height="400px" src="https://github.com/marverenic/Jockey/raw/master/screenshots/Albums9_framed.png">

_ _ _ _ _

### Downloads
You can get Jockey on [Google Play](https://play.google.com/store/apps/details?id=com.marverenic.music), and [opt-in to Beta testing](https://play.google.com/apps/testing/com.marverenic.music) if you want to try new features before they're released.

### Permissions
#### Android 6.0 and higher
**Storage**  
Jockey needs permission to Storage so that it can scan for music and play songs. Without this permission, Jockey can't work and will just kind of stare at you passive-aggressively until you grant this permission
#### Android 5.1 and lower
**Read and write to external storage**  
Used to save local data; primarily used for Last.fm cache, library storage, and other small miscellaneous files.  
**Internet**  
Used to retrieve information and thumbnails for artists from Last.fm and upload anonymous usage and crash data with Crashlytics  
**Network State**  
Used to prevent Jockey from using mobile data (if this preference is enabled)  
**Keep awake**  
Used to play music while the device's screen is off

### Building Jockey from Source
To build a release APK of Jockey, you'll need to either setup Crashlytics using your own API key, or remove the dependency and all logging calls. You can specify your own API key by making a new file in `app/fabric.properties` and add the following lines:  
```
apiSecret="yourApiSecret"
apiKey="yourApiKey"
```

If you want to remove the crashlytics dependency instead, simply delete the `com.marverenic.music.utils.CrashlyticsTree` class, and remove references to `CrashlyticsTree` from `com.marverenic.music.JockeyApplication#setupTimber()`.

### Bugs & contributing
Feel free to post suggestions, bugs, comments on code in the project, or just anything that isn't as smooth as it should be as an issue on Github. You don't need to submit crashes unless you're running a version of Jockey you've built yourself – crashes are automatically reported through Crashlytics. If you're feeling adventerous, you're more than welcome to fork Jockey and submit a pull request either to implement a new feature, fix a bug, or clean up the code.

#### Submitting Feature Requests
Feature requests should be submitted through the Github issue tracker. Before submitting a feature request, make sure that it hasn't been requested before. If it's already been requested, but is in a closed issue that hasn't been marked as "wontfix", feel free to resubmit it in case it's gotten lost.

When submitting a feature request, please make a single issue for each feature request (i.e. don't submit an issue that contains a list of features). Such issues are hard to keep track of and often get lost.

#### Pivotal Tracker
Jockey has a [Pivotal Tracker page](https://www.pivotaltracker.com/n/projects/1594253) which contains the most up-to-date information about planned work. This dashboard is public, but is not open for modification. The best way to add new stories to the dashboard is through Github issues.

Jockey's Pivotal Tracker page includes stories for upcomming features, chores for developer-oriented tasks, bugs, and release markers. Stories are assigned point values using the fibonacci scale to determine roughly how long it will take to implement (1 point is a couple of hours, 2 points is several hours, 3 points is roughly a full day or part of a weekend, 5 points is most of a week, and 8 points is more than a week). These point values are only estimates and shouldn't be treated as guarantees of how long it will take to implement a feature. The time associated with a story's point value can also vary greatly depending on how much time I can devote to the project during the week.

Similarly, release markers, release dates, and prioritizations are very flexible and can change at any time.

### License
Jockey is licensed under an Apache 2.0 license
