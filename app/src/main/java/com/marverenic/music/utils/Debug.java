package com.marverenic.music.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;

import com.marverenic.music.BuildConfig;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

@Deprecated
public class Debug {

    public static final String FILENAME = "jockey.log";

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
