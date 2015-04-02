package com.marverenic.music.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.marverenic.music.BuildConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

public class Updater implements Runnable {

    private Context context;

    public Updater(Context context) {
        this.context = context;
    }

    @Override
    public void run() {
        final Handler handler = new Handler(Looper.getMainLooper());

        // Check with a URL to see if Jockey has an update
        ConnectivityManager network = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        NetworkInfo info = network.getActiveNetworkInfo();

        // Only check for an update if a valid network is present
        if(info != null && info.isAvailable() && !info.isRoaming()
                && (prefs.getBoolean("prefUseMobileData", true) || info.getType() != ConnectivityManager.TYPE_MOBILE)) {
            try {
                URL versionUrl = new URL("https://raw.githubusercontent.com/marverenic/Jockey/master/VERSION");
                BufferedReader in = new BufferedReader(new InputStreamReader(versionUrl.openStream()));

                final String code = in.readLine();
                final String name = in.readLine();

                if (Integer.parseInt(code) > BuildConfig.VERSION_CODE) {
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder alert = new AlertDialog.Builder(context);
                            alert.setTitle("Update available")
                                    .setMessage("A new version of Jockey (version " + name + ") is available to download and install.")
                                    .setPositiveButton("Download now", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            String downloadUrl = "https://sourceforge.net/projects/jockey-player/files/latest/download";
                                            Intent downloadIntent = new Intent(Intent.ACTION_VIEW);
                                            downloadIntent.setData(Uri.parse(downloadUrl));
                                            context.startActivity(downloadIntent);
                                        }
                                    })
                                    .setNegativeButton("Remind me later", null)
                                    .show();
                        }
                    });

                }
                in.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
