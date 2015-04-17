package com.marverenic.music.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;

import com.marverenic.music.PlayerController;
import com.marverenic.music.R;
import com.marverenic.music.adapters.QueueEditAdapter;
import com.marverenic.music.instances.LibraryScanner;
import com.marverenic.music.utils.Themes;
import com.mobeta.android.dslv.DragSortListView;

public class QueueActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setContentLayout(R.layout.page_editable_list);
        setContentView(R.id.list);
        super.onCreate(savedInstanceState);

        // The adapter will initialize attach itself and all necessary controllers in its constructor
        // There is no need to create a variable for it
        new QueueEditAdapter(this, (DragSortListView) findViewById(R.id.list));
    }

    public void update (){
        super.update();
        new QueueEditAdapter(this, (DragSortListView) findViewById(R.id.list));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.queue, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.save:
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                input.setHint("Playlist name");

                final Context context = this;

                new AlertDialog.Builder(this)
                        .setTitle("Save queue as playlist")
                        .setView(input)
                        .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                LibraryScanner.createPlaylist(context, input.getText().toString(), PlayerController.getQueue());
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }).show();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int padding = (int) getResources().getDimension(R.dimen.alert_padding);
                    ((View) input.getParent()).setPadding(
                            padding - input.getPaddingLeft(),
                            padding,
                            padding - input.getPaddingRight(),
                            input.getPaddingBottom());
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void themeActivity() {
        super.themeActivity();

        findViewById(R.id.list).setBackgroundColor(Themes.getBackgroundElevated());

        ListView list = (ListView) findViewById(R.id.list);
        list.setDividerHeight((int) getResources().getDisplayMetrics().density);

        LayerDrawable backgroundDrawable = (LayerDrawable) getResources().getDrawable(R.drawable.header_frame);
        GradientDrawable bodyDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.body));
        GradientDrawable topDrawable = ((GradientDrawable) backgroundDrawable.findDrawableByLayerId(R.id.top));
        bodyDrawable.setColor(Themes.getBackground());
        topDrawable.setColor(Themes.getPrimary());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ((ViewGroup) findViewById(R.id.list).getParent()).setBackground(backgroundDrawable);
        }
        else {
            ((ViewGroup) findViewById(R.id.list).getParent()).setBackgroundDrawable(backgroundDrawable);
        }
    }

    @Override
    public void updateMiniplayer(){
        RelativeLayout.LayoutParams contentLayoutParams = (RelativeLayout.LayoutParams) (findViewById(R.id.list)).getLayoutParams();
        contentLayoutParams.bottomMargin = 0;
        findViewById(R.id.list).setLayoutParams(contentLayoutParams);

        FrameLayout.LayoutParams playerLayoutParams = (FrameLayout.LayoutParams) (findViewById(R.id.miniplayer)).getLayoutParams();
        playerLayoutParams.height = 0;
        findViewById(R.id.miniplayer).setLayoutParams(playerLayoutParams);
    }
}
