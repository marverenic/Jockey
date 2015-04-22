package com.marverenic.music.utils;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import com.marverenic.music.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Calendar;
import java.util.Locale;

public class Debug {

    public enum LogLevel {VERBOSE, INFO, DEBUG, WARNING, ERROR, WTF, WTSF }
    public static final String FILENAME = "jockey.log";

    public static void log(String tag, String message, Context context) {
        // The default level is info
        log(LogLevel.INFO, tag, message, context);
    }

    public static void log(LogLevel level, String tag, String message, final Context context) {
        if (level == LogLevel.WTF) {
            amend("[WTF]\t\t" + Calendar.getInstance().getTime().toString() + "\t\t" + tag + ": " + message + "\n", context);
            Log.wtf(tag, message);

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);

            alertBuilder.setTitle("Something bad happened.")
                    .setMessage("Jockey encountered a problem and may need to be restarted. If you see this message frequently, you should contact the developer.\nAdditional details may be found in Jockey's log file.")
                    .setPositiveButton("Okay", null)
                    .show();
        }
        else if (level == LogLevel.WTSF){
            amend("[WTF]\t\t" + Calendar.getInstance().getTime().toString() + "\t\t" + tag + ": " + message + "\n", context);
            Log.wtf(tag, message);

            AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);

            alertBuilder.setTitle("Something bad happened.")
                    .setMessage("Jockey encountered a serious problem and should be restarted. If this isn't the first time you've seen this message, you should contact the developer.\nAdditional details may be found in Jockey's log file.")
                    .setPositiveButton("Okay", null)
                    .show();
        }
        else if (BuildConfig.DEBUG) {
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

    public static void log(Throwable t, Context context) {
        StringWriter stackTrace = new StringWriter();
        t.printStackTrace(new PrintWriter(stackTrace));
        amend(stackTrace.toString() + getHeader(context), context);
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

    private static String getHeader(Context context){
        ConnectivityManager network = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = network.getActiveNetworkInfo();

        String RAM;

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            ActivityManager actManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
            actManager.getMemoryInfo(memInfo);

            RAM = memInfo.totalMem/1048576 + " MB";
        }
        else{
            RAM = "Unknown (API < 16)";
        }

        return  "\n" +
                "[DEVICE INFO]" + "\n" +
                "Jockey " + BuildConfig.VERSION_NAME + " (build " + BuildConfig.VERSION_CODE + ")" + "\n" +
                Build.BRAND + " " + Build.MODEL + "\n" +
                "Android version " + Build.VERSION.RELEASE + "\n" +
                "Java max heap size: " + Runtime.getRuntime().maxMemory()/1048576 + "MB\n" +
                "Device memory: " + RAM + "\n" +
                "Locale: " + Locale.getDefault() + "\n" +
                "Network Status: " + ((info == null)
                    ? "Unavailable"
                    : "type: " + info.getTypeName() + ", state: " + info.getState()) + "\n";
    }

}
