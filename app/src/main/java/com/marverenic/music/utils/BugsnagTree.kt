package com.marverenic.music.utils

import android.app.ActivityManager
import android.content.Context
import android.util.Log.*
import androidx.core.content.getSystemService
import com.bugsnag.android.Bugsnag
import com.bugsnag.android.Severity
import timber.log.Timber
import java.util.*
import java.util.Locale.US

class BugsnagTree(
    context: Context,
    private val bufferSize: Int = 200
) : Timber.Tree() {

    private val processName = context.currentProcessName()
    private val logs = ArrayDeque<String>(bufferSize + 1)

    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        throwable: Throwable?
    ) {
        if (priority != VERBOSE && priority != DEBUG) {
            addLog(message, priority)
            throwable?.let { logThrowable(it, priority) }
        }
    }

    private fun addLog(message: String, priority: Int) {
        val log = "${System.currentTimeMillis()} ${priority.toSeverityString()}: $message"

        synchronized(logs) {
            logs.add(log)
            if (logs.size > bufferSize) {
                logs.removeFirst()
            }
        }
    }

    private fun logThrowable(throwable: Throwable, priority: Int) {
        val severity = when (priority) {
            ERROR -> Severity.ERROR
            WARN -> Severity.WARNING
            INFO -> Severity.INFO
            else -> return
        }

        Bugsnag.notify(throwable) { report ->
            val error = report.error ?: return@notify

            error.severity = severity
            error.addToTab("App", "processName", processName)
            synchronized(logs) {
                logs.forEachIndexed { index, log ->
                    error.addToTab("Log", String.format(US, "%03d", index), log)
                }
            }
        }
    }

    private fun Int.toSeverityString(): String {
        return when (this) {
            ERROR -> "E"
            WARN -> "W"
            INFO -> "I"
            DEBUG -> "D"
            VERBOSE -> "V"
            else -> toString()
        }
    }

    private fun Context.currentProcessName(): String? {
        val pid = android.os.Process.myPid()
        return getSystemService<ActivityManager>()
            ?.runningAppProcesses
            ?.firstOrNull { it.pid == pid }
            ?.processName
    }
}
