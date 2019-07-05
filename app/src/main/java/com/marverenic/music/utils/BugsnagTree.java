package com.marverenic.music.utils;

import android.app.ActivityManager;
import android.content.Context;

import com.bugsnag.android.Bugsnag;
import com.bugsnag.android.Error;
import com.bugsnag.android.Severity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

import timber.log.Timber;

import static android.util.Log.DEBUG;
import static android.util.Log.ERROR;
import static android.util.Log.INFO;
import static android.util.Log.VERBOSE;
import static android.util.Log.WARN;
import static java.util.Locale.US;

public class BugsnagTree extends Timber.Tree {

    private static final int BUFFER_SIZE = 200;

    private final String processName;
    private final ArrayDeque<String> logs = new ArrayDeque<>(BUFFER_SIZE + 1);

    public BugsnagTree(Context context) {
        processName = currentProcessName(context);
    }

    @Override
    protected void log(int priority, @Nullable String tag, @Nullable String message,
                       @Nullable Throwable throwable) {
        if (priority == VERBOSE || priority == DEBUG) {
            return;
        }

        if (message != null) {
            String log = System.currentTimeMillis() + " "
                    + getPrioritySymbol(priority) + ": " + message;

            synchronized (logs) {
                logs.add(log);
                if (logs.size() > BUFFER_SIZE) {
                    logs.removeFirst();
                }
            }
        }

        if (throwable != null) {
            Severity severity;
            if (priority == ERROR) {
                severity = Severity.ERROR;
            } else if (priority == WARN) {
                severity = Severity.WARNING;
            } else if (priority == INFO) {
                severity = Severity.INFO;
            } else {
                return;
            }

            Bugsnag.notify(throwable, report -> {
                Error error = report.getError();
                error.setSeverity(severity);
                error.addToTab("App", "processName", processName);
                synchronized (logs) {
                    int i = 0;
                    for (String log : logs) {
                        error.addToTab("Log", String.format(US, "%03d", i++), log);
                    }
                }
            });
        }
    }

    private String getPrioritySymbol(int priority) {
        switch (priority) {
            case ERROR: return "E";
            case WARN: return "W";
            case INFO: return "I";
            case DEBUG: return "D";
            case VERBOSE: return "V";
            default: return Integer.toString(priority);
        }
    }

    private static String currentProcessName(Context context) {
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
            if (processInfo.pid == pid) {
                return processInfo.processName;
            }
        }
        return null;
    }
}
