package com.marverenic.music.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;

public class Debug {

    public static final byte VERBOSE = 0;
    public static final byte INFO = 1;
    public static final byte DEBUG = 2;
    public static final byte WARNING = 3;
    public static final byte ERROR = 4;
    public static final byte WTF = 5;
    private static final String FILENAME = "jockey.log";
    public static byte debugLevel = 0;

    public static void log(String tag, String message, Context context) {
        // The default level is info
        log(INFO, tag, message, context);
    }

    public static void log(byte level, String tag, String message, Context context) {
        if (level == WTF) {
            amend("[WTF]\t\t" + Calendar.getInstance().getTime().toString() + "\t\t" + tag + ": " + message + "\n", context);
            Log.wtf(tag, message);

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);

            alertBuilder.setTitle("Something bad happened.")
                    .setMessage("Jockey encountered a serious problem and may need to be restarted.\nAdditional details may be found in Jockey's log file.")
                    .setPositiveButton("Okay", null)
                    .show();
        }
        //else if (level >= debugLevel && !(!BuildConfig.DEBUG && debugLevel == VERBOSE)) {
        else if (level >= debugLevel) {
            String line;

            switch (level) {
                case VERBOSE:
                    Log.v(tag, message);
                    line = "[VERBOSE]\t";
                    break;
                case INFO:
                    Log.i(tag, message);
                    line = "[INFO]\t\t";
                    break;
                case DEBUG:
                    Log.d(tag, message);
                    line = "[DEBUG]\t\t";
                    break;
                case WARNING:
                    Log.w(tag, message);
                    line = "[WARNING]\t";
                    break;
                case ERROR:
                    Log.e(tag, message);
                    line = "[ERROR]\t\t";
                    break;
                default:
                    Log.w(tag, message);
                    Log.w(tag, "Additionally, an invalid log level was used");
                    line = "[UNKNOWN]\t";
            }

            line += Calendar.getInstance().getTime().toString() + "\t\t" + tag + ": " + message + "\n";
            amend(line, context);
        }
    }

    private static void amend(String line, Context context) {
        File logFile = new File(context.getExternalFilesDir(null), FILENAME);

        try {
            FileOutputStream outputStream = new FileOutputStream(logFile, true);
            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write(line);
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
