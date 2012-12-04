package com.chanapps.four.component;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Point;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;
import com.chanapps.four.activity.*;
import com.chanapps.four.data.*;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.ImageScaleType;

/**
 * Created with IntelliJ IDEA.
 * User: arley
 * Date: 11/23/12
 * Time: 3:58 PM
 * To change this template use File | Settings | File Templates.
 */
public class ChanViewHelper {

    public enum ViewType {
        LIST,
        GRID
    }

    public enum ServiceType {
        BOARD,
        THREAD,
        WATCHLIST
    }

    private static final String TAG = ChanViewHelper.class.getSimpleName();

    private Activity activity;
    private DisplayImageOptions options;
    private ImageLoader imageLoader;
    private String boardCode;
    private long threadNo = 0;
    private String text;
    private String imageUrl;
    private int imageWidth;
    private int imageHeight;
    private boolean hideAllText = false;
    private ServiceType serviceType;

    public ChanViewHelper(Activity activity, ServiceType serviceType) {
        this(activity, getViewTypeFromOrientation(activity), serviceType);
    }

    public ChanViewHelper(Activity activity, ViewType viewType, ServiceType serviceType) {
        this.activity = activity;
        this.serviceType = serviceType;
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(activity));
        if (viewType == ViewType.LIST) {
            options = new DisplayImageOptions.Builder()
    // for text-only posts this image is still there, causing display problems
    //			.showImageForEmptyUri(R.drawable.stub_image)
    			.cacheOnDisc()
    			.imageScaleType(ImageScaleType.EXACT)
    			.build();
            }
        else {
            options = new DisplayImageOptions.Builder()
                .showImageForEmptyUri(R.drawable.stub_image)
			    .cacheOnDisc()
			    .imageScaleType(ImageScaleType.EXACT)
			    .build();
        }
    }

    public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
        String rawText = cursor.getString(cursor.getColumnIndex(ChanDatabaseHelper.POST_TEXT));
        String rawImageUrl = cursor.getString(cursor.getColumnIndex(ChanDatabaseHelper.POST_IMAGE_URL));
        String text = rawText == null ? "" : rawText;
        String imageUrl = rawImageUrl == null ? "" : rawImageUrl;
        if (view instanceof TextView) {
            //todo - @john - if the text is hidden then the image should take the full available space.
            TextView tv = (TextView) view;
            Log.v(TAG, "setting text: " + text);
            switch (getViewType()) {
                case GRID:
                    if (hideAllText || text == null || text.isEmpty()) {
                        tv.setVisibility(View.INVISIBLE);
                    }
                    else {
                        setGridViewText(tv, text, cursor);
                    }
                    break;
                default:
                    setListViewText(tv, text, cursor);
                    break;
            }
            return true;
        } else if (view instanceof ImageView) {
            ImageView iv = (ImageView) view;
            // making this invisible causes display problems
            //if (imageUrl != null && !imageUrl.isEmpty()) {
                setViewImage(iv, imageUrl, cursor);
            //}
            //else {
            //    iv.setVisibility(View.INVISIBLE);
            //}
            return true;
        } else {
            return false;
        }
    }

    public void setListViewText(TextView textView, String text, Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "setViewText - Why is cursor null?");
            return;
        }
        int tn_w = cursor.getInt(cursor.getColumnIndex(ChanDatabaseHelper.POST_TN_W));
        int tn_h = cursor.getInt(cursor.getColumnIndex(ChanDatabaseHelper.POST_TN_H));
        //Log.i(TAG, "tn_w=" + tn_w + ", tn_h=" + tn_h);
        Point imageDimensions = new Point(tn_w, tn_h);
        if (imageDimensions != null && imageDimensions.x > 0 && imageDimensions.y > 0) {
            text = text == null ? "" : text;
            FlowTextHelper.tryFlowText(text, imageDimensions, textView);
        } else {
            textView.setText(text);
        }
    }

    public void setGridViewText(TextView textView, String text, Cursor cursor) {
        if (cursor == null) {
            Log.w(TAG, "setViewText - Why is cursor null?");
            return;
        }
        text = text.substring(0, Math.min(text.length(), 22));
        textView.setText(text);
    }

    public void setViewImage(ImageView imageView, final String thumbnailImageUrl, Cursor cursor) {
        try {
            this.imageLoader.displayImage(thumbnailImageUrl, imageView, options);
        } catch (NumberFormatException nfe) {
            imageView.setImageURI(Uri.parse(thumbnailImageUrl));
        }
    }

    private void setBoardMenu() {
        ActionBar a = activity.getActionBar();
        if (a == null) {
            return;
        }
        int stringId = threadNo == 0
                ? R.string.board_activity
                : R.string.thread_activity;
        String title = "/" + boardCode + " " + activity.getString(stringId);
        a.setTitle(title);
        a.setDisplayHomeAsUpEnabled(true);
    }

    private void reloadPrefs(SharedPreferences prefs) {
        if (prefs == null) {
            prefs = activity.getSharedPreferences(ChanHelper.PREF_NAME, 0);
        }
        hideAllText = prefs.getBoolean(SettingsActivity.PREF_HIDE_ALL_TEXT, false);
    }

    public void onRefresh() {
        reloadPrefs(null);
    }

    private void loadBoardCode(SharedPreferences prefs) {
        String oldBoardCode = prefs.getString(ChanHelper.BOARD_CODE, "s");
        boardCode = "s";
        Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra(ChanHelper.BOARD_CODE)) {
            boardCode = intent.getStringExtra(ChanHelper.BOARD_CODE);
            if (boardCode == null || boardCode.isEmpty()) {
                boardCode = "s";
            }
            Log.i(TAG, "Board code read from intent: " + boardCode);
        }
        if (intent == null || !intent.hasExtra(ChanHelper.BOARD_CODE) || !ChanBoard.isValidBoardCode(activity, boardCode)) {
            boardCode = oldBoardCode;
            Log.i(TAG, "Board code loaded from prefs: " + boardCode);
        }
        if (!oldBoardCode.equals(boardCode)) {
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.BOARD_CODE, boardCode);
            ed.commit();
        }
    }

    private void loadThreadNo(SharedPreferences prefs) {
        long oldThreadNo = prefs.getLong(ChanHelper.THREAD_NO, 0);
        threadNo = 0;
        Intent intent = activity.getIntent();
        if (intent != null && intent.hasExtra(ChanHelper.THREAD_NO)) {
            threadNo = intent.getLongExtra(ChanHelper.THREAD_NO, 0);
            Log.i(TAG, "Thread no read from intent: " + threadNo);
        }
        if (intent == null || !intent.hasExtra(ChanHelper.THREAD_NO)) {
            threadNo = oldThreadNo;
            Log.i(TAG, "Thread no loaded from prefs: " + threadNo);
        }
        if (oldThreadNo != threadNo) {
            text = intent.getStringExtra(ChanHelper.TEXT);
            imageUrl = intent.getStringExtra(ChanHelper.IMAGE_URL);
            imageWidth = intent.getIntExtra(ChanHelper.IMAGE_WIDTH, 0);
            imageHeight = intent.getIntExtra(ChanHelper.IMAGE_HEIGHT, 0);
            SharedPreferences.Editor ed = prefs.edit();
            ed.putString(ChanHelper.BOARD_CODE, boardCode);
            ed.putLong(ChanHelper.THREAD_NO, threadNo);
            ed.putString(ChanHelper.TEXT, text);
            ed.putString(ChanHelper.IMAGE_URL, imageUrl);
            ed.putInt(ChanHelper.IMAGE_WIDTH, imageWidth);
            ed.putInt(ChanHelper.IMAGE_HEIGHT, imageHeight);
            ed.commit();
        }
    }

    public ViewType getViewType() {
        return getViewTypeFromOrientation(activity);
    }

    public void startService() {
        startService(serviceType);
    }

    private void startService(ServiceType serviceType) {
        if (serviceType == ServiceType.WATCHLIST) {
            startWatchlistService();
            return;
        }
        SharedPreferences prefs = activity.getSharedPreferences(ChanHelper.PREF_NAME, 0);
        reloadPrefs(prefs);
        loadBoardCode(prefs);
        if (serviceType == ServiceType.THREAD) {
            loadThreadNo(prefs);
        }
        else {
            threadNo = 0;
        }
        setBoardMenu();
        Log.i(TAG, "Starting ChanLoadService");
        Intent threadIntent = new Intent(activity, ChanLoadService.class);
        threadIntent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        threadIntent.putExtra(ChanHelper.THREAD_NO, threadNo);
        activity.startService(threadIntent);
    }

    private void startWatchlistService() {
        Log.i(TAG, "Starting ChanLoadWatchlistService");
        Intent intent = new Intent(activity, ChanLoadWatchlistService.class);
        activity.startService(intent);
    }

    public static final void startBoardActivity(AdapterView<?> adapterView, View view, int position, long id, Activity activity, String boardCode) {
        Intent intent = new Intent(activity, BoardActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        activity.startActivity(intent);
    }

    public void startThreadActivity(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanDatabaseHelper.POST_ID));
        final String boardName = cursor.getString(cursor.getColumnIndex(ChanDatabaseHelper.POST_BOARD_NAME));
        final String text = cursor.getString(cursor.getColumnIndex(ChanDatabaseHelper.POST_TEXT));
        final String imageUrl = cursor.getString(cursor.getColumnIndex(ChanDatabaseHelper.POST_IMAGE_URL));
        final int tn_w = cursor.getInt(cursor.getColumnIndex(ChanDatabaseHelper.POST_TN_W));
        final int tn_h = cursor.getInt(cursor.getColumnIndex(ChanDatabaseHelper.POST_TN_H));
        Intent intent = new Intent(activity, ThreadActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardName != null ? boardName : boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, postId);
        intent.putExtra(ChanHelper.TEXT, text);
        intent.putExtra(ChanHelper.IMAGE_URL, imageUrl);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, tn_w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, tn_h);
        Log.i(TAG, "Calling thread activity with id=" + id);
        activity.startActivity(intent);
    }

    public void startFullImageActivity(AdapterView<?> adapterView, View view, int position, long id) {
        Cursor cursor = (Cursor) adapterView.getItemAtPosition(position);
        final long postId = cursor.getLong(cursor.getColumnIndex(ChanDatabaseHelper.POST_ID));
        final int w = cursor.getInt(cursor.getColumnIndex(ChanDatabaseHelper.POST_W));
        final int h = cursor.getInt(cursor.getColumnIndex(ChanDatabaseHelper.POST_H));
        Intent intent = new Intent(activity, FullScreenImageActivity.class);
        intent.putExtra(ChanHelper.BOARD_CODE, boardCode);
        intent.putExtra(ChanHelper.THREAD_NO, threadNo);
        intent.putExtra(ChanHelper.POST_NO, postId);
        intent.putExtra(ChanHelper.IMAGE_WIDTH, w);
        intent.putExtra(ChanHelper.IMAGE_HEIGHT, h);
        activity.startActivity(intent);
    }

    public String getBoardCode() {
        return boardCode;
    }

    public long getThreadNo() {
        return threadNo;
    }

    public String getText() {
        return text;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public ViewType getViewTypeFromOrientation() {
        return getViewTypeFromOrientation(activity);
    }

    public static final ViewType getViewTypeFromOrientation(Context activity) {
        return
                ChanHelper.getOrientation(activity) == ChanHelper.Orientation.PORTRAIT
                ? ChanViewHelper.ViewType.GRID
                : ChanViewHelper.ViewType.LIST;
    }
}
