package com.marverenic.music.utils;

import android.os.AsyncTask;

public class BackgroundTask extends AsyncTask<Void, Void, Void> {

    private BackgroundAction backgroundAction = null;
    private OnCompleteAction onCompleteAction = null;

    public BackgroundTask (BackgroundAction backgroundAction, OnCompleteAction onCompleteAction){
        if (backgroundAction == null){
            throw new NullPointerException("BackgroundAction cannot be null");
        }

        this.backgroundAction = backgroundAction;
        this.onCompleteAction = onCompleteAction;
        execute(null, null, null);
    }

    public interface BackgroundAction{
        void doInBackground();
    }

    public interface OnCompleteAction{
        void onComplete();
    }

    @Override
    protected Void doInBackground(Void... params) {
        backgroundAction.doInBackground();
        return null;
    }

    @Override
    protected void onPostExecute(Void result){
        if (result == null) return;

        onCompleteAction.onComplete();
    }
}
