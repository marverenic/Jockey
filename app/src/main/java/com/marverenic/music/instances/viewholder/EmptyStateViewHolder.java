package com.marverenic.music.instances.viewholder;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.StringRes;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.marverenic.music.BuildConfig;
import com.marverenic.music.instances.Library;
import com.marverenic.music.R;

public class EmptyStateViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

    private Activity activity;
    private View itemView;

    public EmptyStateViewHolder(View itemView, Activity activity){
        super(itemView);
        this.activity = activity;
        this.itemView = itemView;

        if (!Library.hasRWPermission(activity)){
            setReason(R.string.empty_no_permission);
            setDetail(R.string.empty_no_permission_detail);
            setButton1(R.string.action_try_again);
            setButton2(R.string.action_open_settings);
        }
    }

    public void setReason(@StringRes int message){
        setReason(activity.getString(message));
    }

    public void setReason(String message){
        TextView messageView = (TextView) itemView.findViewById(R.id.empty_message);
        messageView.setText(message);
    }

    public void setDetail(@StringRes int message){
        setDetail(activity.getString(message));
    }

    public void setDetail(String message){
        TextView detailView = (TextView) itemView.findViewById(R.id.empty_message_detail);
        detailView.setText(message);
    }

    public void setButton1(@StringRes int button1Label){
        setButton1(activity.getString(button1Label));
    }

    public void setButton1(String button1Label){
        View button = itemView.findViewById(R.id.empty_button);
        TextView buttonLabel = (TextView) itemView.findViewById(R.id.empty_button_label);
        buttonLabel.setText(button1Label);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(this);
    }

    public void removeButton1(){
        View button1 = itemView.findViewById(R.id.empty_button);
        button1.setVisibility(View.GONE);
    }

    public void setButton2(@StringRes int button2Label){
        setButton2(activity.getString(button2Label));
    }

    public void setButton2(String button2Label){
        View button = itemView.findViewById(R.id.empty_button2);
        TextView buttonLabel = (TextView) itemView.findViewById(R.id.empty_button2_label);
        buttonLabel.setText(button2Label);
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(this);
    }

    public void removeButton2(){
        View button = itemView.findViewById(R.id.empty_button2);
        button.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.empty_button:
                Library.scanAll(activity);
                break;
            case R.id.empty_button2:
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null);
                intent.setData(uri);
                activity.startActivity(intent);
                break;
        }
    }
}
