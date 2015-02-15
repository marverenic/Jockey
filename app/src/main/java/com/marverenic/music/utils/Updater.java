package com.marverenic.music.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

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

        // Check for versions of Jockey published by ensiluxrum
        PackageManager pm = context.getPackageManager();
        boolean hasOldVersion;
        try {
            pm.getPackageInfo("com.ensiluxrum.music", PackageManager.GET_ACTIVITIES);
            hasOldVersion = true;
        } catch (PackageManager.NameNotFoundException e) {
            hasOldVersion = false;
        }

        if (hasOldVersion) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    new AlertDialog.Builder(context)
                            .setTitle("An old version of Jockey was found")
                            .setMessage("You should uninstall it to avoid conflicts and general confusion.")
                            .setPositiveButton("Uninstall it for me", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Intent intent = new Intent(Intent.ACTION_DELETE);
                                    intent.setData(Uri.parse("package:com.ensiluxrum.music"));
                                    context.startActivity(intent);
                                }
                            })
                            .setNeutralButton("Maybe later", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            })
                            .show();
                }
            });
        }


        // Check with a URL to see if Jockey has an update
        try {
            URL versionUrl = new URL("https://raw.githubusercontent.com/marverenic/Jockey/master/VERSION");
            BufferedReader in = new BufferedReader(new InputStreamReader(versionUrl.openStream()));

            final String code = in.readLine();
            final String name = in.readLine();

            if (Integer.parseInt(code) > context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionCode) {
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
