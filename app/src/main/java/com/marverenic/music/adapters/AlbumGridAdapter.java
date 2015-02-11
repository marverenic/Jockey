package com.marverenic.music.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v7.graphics.Palette;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

import com.marverenic.music.LibraryPageActivity;
import com.marverenic.music.Player;
import com.marverenic.music.R;
import com.marverenic.music.instances.Album;
import com.marverenic.music.instances.Song;
import com.marverenic.music.utils.Themes;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.util.ArrayList;
import java.util.HashMap;

public class AlbumGridAdapter extends BaseAdapter implements ImageLoadingListener, View.OnClickListener, View.OnLongClickListener {

    private ArrayList<Album> data;
    private Context mContext;
    private HashMap<String, int[]> colorTable = new HashMap<>();

    public AlbumGridAdapter(ArrayList<Album> data, Context context) {
        this.data = data;
        mContext = context;

        if (!ImageLoader.getInstance().isInited()) {
            int albumSizePx = 100 * (int) context.getResources().getDisplayMetrics().density;
            ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(context)
                    .defaultDisplayImageOptions((
                            new DisplayImageOptions.Builder()
                                    .cacheInMemory(true)
                                    .cacheOnDisk(true)
                    ).build())
                    .memoryCacheSizePercentage(20)
                    .diskCacheSize(20 * 1024 * 1024)
                    .memoryCacheExtraOptions(albumSizePx, albumSizePx)
                    .diskCacheExtraOptions(albumSizePx, albumSizePx, null)
                    .build();
            ImageLoader.getInstance().init(config);
        }

    }

    @Override
    public int getCount() {
        return data.size();
    }

    @Override
    public Object getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        AlbumViewHolder viewHolder;
        Album a = data.get(position);

        if (convertView == null) {
            // inflate the GridView item layout
            LayoutInflater inflater = LayoutInflater.from(mContext);
            convertView = inflater.inflate(R.layout.instance_album, parent, false);
            convertView.findViewById(R.id.albumInstance).setOnClickListener(this);
            convertView.findViewById(R.id.albumInstance).setOnLongClickListener(this);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ((FrameLayout) convertView).setForeground(Themes.getTouchRipple(mContext));
            }

            // initialize the view holder
            viewHolder = new AlbumViewHolder();
            viewHolder.art = (ImageView) convertView.findViewById(R.id.imageAlbumArt);
            viewHolder.title = (TextView) convertView.findViewById(R.id.textAlbumTitle);
            viewHolder.detail = (TextView) convertView.findViewById(R.id.textAlbumArtist);
            viewHolder.parent = (ViewGroup) convertView.findViewById(R.id.albumBackground);
            convertView.setTag(viewHolder);
        } else {
            // recycle the already inflated view
            viewHolder = (AlbumViewHolder) convertView.getTag();
            ImageLoader.getInstance().cancelDisplayTask(viewHolder.art);
        }

        viewHolder.title.setText(a.albumName);
        viewHolder.detail.setText(a.artistName);

        if (a.artUri != null) {
            if (colorTable.get("file://" + a.artUri) != null) {
                viewHolder.art.setImageDrawable(new ColorDrawable(colorTable.get("file://" + a.artUri)[0]));
                viewHolder.parent.setBackgroundColor(colorTable.get("file://" + a.artUri)[0]);
                viewHolder.title.setTextColor(colorTable.get("file://" + a.artUri)[1]);
                viewHolder.detail.setTextColor(colorTable.get("file://" + a.artUri)[2]);
            } else {
                viewHolder.art.setImageResource(R.color.grid_background_default);
                viewHolder.parent.setBackgroundColor(mContext.getResources().getColor(R.color.grid_background_default));
                viewHolder.title.setTextColor(mContext.getResources().getColor(R.color.grid_text));
                viewHolder.detail.setTextColor(mContext.getResources().getColor(R.color.grid_detail_text));
            }
            ImageLoader.getInstance().displayImage("file://" + a.artUri, viewHolder.art, this);
        } else {
            viewHolder.art.setImageResource(R.drawable.art_default);
            viewHolder.parent.setBackgroundColor(mContext.getResources().getColor(R.color.grid_background_default));
            viewHolder.title.setTextColor(mContext.getResources().getColor(R.color.grid_text));
            viewHolder.detail.setTextColor(mContext.getResources().getColor(R.color.grid_detail_text));
        }

        return convertView;
    }

    @Override
    public void onLoadingStarted(String imageUri, View view) {
    }

    @Override
    public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
        Log.e("AlbumGridAdapter", "Failed to load image from URI: " + imageUri + "\nReason: " + failReason);
    }

    @Override
    public void onLoadingComplete(final String imageUri, final View view, final Bitmap loadedImage) {
        AlbumViewHolder viewHolder = new AlbumViewHolder();
        viewHolder.art = (ImageView) view;
        viewHolder.parent = (ViewGroup) view.getParent();
        viewHolder.title = (TextView) viewHolder.parent.getChildAt(1);
        viewHolder.detail = (TextView) viewHolder.parent.getChildAt(2);

        if (colorTable.get(imageUri) == null) {
            final AlbumViewHolder vh = viewHolder;

            Palette.generateAsync(loadedImage, new Palette.PaletteAsyncListener() {
                @Override
                public void onGenerated(Palette palette) {
                    int backgroundColor = mContext.getResources().getColor(R.color.grid_background_default);
                    int titleTextColor = mContext.getResources().getColor(R.color.grid_text);
                    int detailTextColor = mContext.getResources().getColor(R.color.grid_detail_text);


                    if (palette.getVibrantSwatch() != null && palette.getVibrantColor(-1) != -1) {
                        backgroundColor = palette.getVibrantColor(0);
                        titleTextColor = palette.getVibrantSwatch().getTitleTextColor();
                        detailTextColor = palette.getVibrantSwatch().getBodyTextColor();
                    } else if (palette.getLightVibrantSwatch() != null && palette.getLightVibrantColor(-1) != -1) {
                        backgroundColor = palette.getLightVibrantColor(0);
                        titleTextColor = palette.getLightVibrantSwatch().getTitleTextColor();
                        detailTextColor = palette.getLightVibrantSwatch().getBodyTextColor();
                    } else if (palette.getDarkVibrantSwatch() != null && palette.getDarkVibrantColor(-1) != -1) {
                        backgroundColor = palette.getDarkVibrantColor(0);
                        titleTextColor = palette.getDarkVibrantSwatch().getTitleTextColor();
                        detailTextColor = palette.getDarkVibrantSwatch().getBodyTextColor();
                    } else if (palette.getLightMutedSwatch() != null && palette.getLightMutedColor(-1) != -1) {
                        backgroundColor = palette.getLightMutedColor(0);
                        titleTextColor = palette.getLightMutedSwatch().getTitleTextColor();
                        detailTextColor = palette.getLightMutedSwatch().getBodyTextColor();
                    } else if (palette.getDarkMutedSwatch() != null && palette.getDarkMutedColor(-1) != -1) {
                        backgroundColor = palette.getDarkMutedColor(0);
                        titleTextColor = palette.getDarkMutedSwatch().getTitleTextColor();
                        detailTextColor = palette.getDarkMutedSwatch().getBodyTextColor();
                    }

                    colorTable.put(imageUri, new int[]{backgroundColor, titleTextColor, detailTextColor});

                    vh.parent.setBackgroundColor(backgroundColor);
                    vh.title.setTextColor(titleTextColor);
                    vh.detail.setTextColor(detailTextColor);
                }
            });
        }
    }

    @Override
    public void onLoadingCancelled(String imageUri, View view) {
    }

    @Override
    public void onClick(View v) {
        int position = ((GridView) v.getParent()).getPositionForView(v);
        Album album = data.get(position);

        Intent intent = new Intent(mContext, LibraryPageActivity.class);
        intent.putExtra("entry", album);

        mContext.startActivity(intent);
    }

    @Override
    public boolean onLongClick(View view) {
        final Album item = data.get(((GridView) view.getParent()).getPositionForView(view));
        final ArrayList<Song> contents = new ArrayList<>();

        AlertDialog.Builder dialog = new AlertDialog.Builder(mContext);

        dialog.setTitle(item.albumName)
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // There's nothing to do here
                    }
                })
                .setItems(R.array.queue_options_album, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0 || which == 1) {
                            Cursor cur = mContext.getContentResolver().query(
                                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                    new String[]{
                                            MediaStore.Audio.Media.TITLE,
                                            MediaStore.Audio.Media.ARTIST,
                                            MediaStore.Audio.Media.ALBUM,
                                            MediaStore.Audio.Media.DURATION,
                                            MediaStore.Audio.Media.DATA,
                                            MediaStore.Audio.Media.ALBUM_ID},
                                    MediaStore.Audio.Media.IS_MUSIC + "!= 0 AND " + MediaStore.Audio.Media.ALBUM_ID + "=?",
                                    new String[]{(item).albumId + ""},
                                    MediaStore.Audio.Media.TRACK);
                            cur.moveToFirst();

                            for (int i = 0; i < cur.getCount(); i++) {
                                cur.moveToPosition(i);
                                contents.add(new Song(
                                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)),
                                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ARTIST)),
                                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM)),
                                        cur.getInt(cur.getColumnIndex(MediaStore.Audio.Media.DURATION)),
                                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)),
                                        cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID))));
                            }
                        }
                        switch (which) {
                            case 0: //Queue this artist next
                                Player.getInstance().queueNext(contents);
                                break;
                            case 1: //Queue this artist last
                                Player.getInstance().queueLast(contents);
                                break;
                            default:
                                break;
                        }
                    }
                });

        dialog.create().show();
        return true;
    }

    public void updateData(ArrayList<Album> data) {
        this.data = data;
        notifyDataSetChanged();
    }

    public class AlbumViewHolder {
        public ImageView art;
        public TextView title;
        public TextView detail;
        public ViewGroup parent;
    }
}
