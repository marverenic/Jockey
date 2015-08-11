# Jockey
Jockey is a music player for Android based on Google's Material Design standards. The point of Jockey is to be a simple, lightweight media player designed for normal people (meaning that if you need an EQ, gapless playback, crossfade, or replay gain, Jockey probably isn't for you).


![Default library view](http://i.imgur.com/tFZTUU2.png)
![Designed for tablets](http://i.imgur.com/VYxek4n.png)

Also, Jockey was coded by a college student with only one semester of Computer Science. That being said, if you decide to modify Jockey, I apologize in advance.

### Downloads
To download compiled versions of Jockey, visit the [build branch](https://github.com/marverenic/Jockey/tree/build).  
In order to install comiled APK's, you will need to enable installation of applications from third party sources if you haven't already done so, and then sideload the APK. There are many guides available online that demonstrate this process in detail.

### Permissions
**Read and write to external storage**  
Used to save local data; primarily used for Last.fm cache, library storage, and other small miscellaneous files.  
**Internet**  
Used to retrieve information and thumbnails for artists from Last.fm and upload anonymous usage and crash data with Crashlytics
**Network State**  
Used to prevent Jockey from using mobile data (if this preference is enabled)  
**Keep awake**  
Used to play music while the device's screen is off  
**Install shortcuts**  
Allows Jockey to (optionally) add shortcuts to Jockey to the launcher. This is only done when explicitly requested from the settings page and is intended so that the launcher icon matches the chosen theme

### Setting up the project
 - Download, install and launch [Android Studio]
 - Clone the repository
 - In Android Studio, select "Import Project..." from the file menu
 - Select Jockey's repository that you just cloned
 - Wait while Gradle begins to build the project (Android Studio may need to restart)

### Bugs & contributing
Feel free to post suggestions, crashes, or just anything that isn't as smooth as it should be to the bug tracker -- I want Jockey to be as seamless as it possibly can. Additionally, don't hesitate to fork Jockey or submit a pull request -- especially if it's a bug fix or cleans up code that's doing mischevious things that I'm not aware of like planning world domination.

### License
Jockey is licensed under an Apache 2.0 license

[Android Studio]:http://developer.android.com/sdk/index.html
[issues page]:https://github.com/marverenic/Jockey/issues
