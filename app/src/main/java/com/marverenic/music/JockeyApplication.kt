package com.marverenic.music

import android.app.Application
import android.content.Context
import androidx.fragment.app.Fragment
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Configuration
import com.bumptech.glide.Glide
import com.marverenic.music.data.inject.JockeyComponentFactory
import com.marverenic.music.data.inject.JockeyGraph
import com.marverenic.music.utils.BugsnagTree
import com.marverenic.music.utils.compat.JockeyPreferencesCompat
import com.marverenic.music.utils.compat.PlayerQueueMigration
import timber.log.Timber

class JockeyApplication : Application() {

    private val component: JockeyGraph by lazy {
        JockeyComponentFactory.create(this)
    }

    override fun onCreate() {
        super.onCreate()

        setupBugsnag()
        setupTimber()

        JockeyPreferencesCompat.upgradeSharedPreferences(this)
        PlayerQueueMigration(this).migrateLegacyQueueFile()
    }

    private fun setupBugsnag() {
        if (BuildConfig.BUGSNAG_ENABLED) {
            Bugsnag.start(this, Configuration.load(this).apply {
                apiKey = BuildConfig.BUGSNAG_API_KEY
                autoDetectErrors = true
            })
        }
    }

    private fun setupTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        if (BuildConfig.BUGSNAG_ENABLED) {
            Timber.plant(BugsnagTree(this))
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Glide.with(this).onTrimMemory(level)
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Glide.with(this).onLowMemory()
    }

    companion object {
        @JvmStatic
        fun getComponent(fragment: Fragment): JockeyGraph {
            return getComponent(fragment.requireContext())
        }

        @JvmStatic
        fun getComponent(context: Context): JockeyGraph {
            val appContext = requireNotNull(context.applicationContext as? JockeyApplication) {
                "Cannot get an instance of the Dagger component: $context's application context " +
                        "is not an instance of JockeyApplication (it is actually " +
                        "${context.applicationContext})"
            }
            return appContext.component
        }
    }
}
