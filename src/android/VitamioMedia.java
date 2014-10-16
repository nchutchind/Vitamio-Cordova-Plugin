package com.hutchind.cordova.plugins.vitamio;

import android.app.Activity;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Point;
import android.view.Display;
import android.view.SurfaceHolder;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

public class VitamioMedia extends Activity implements ImageLoadTaskListener,
        OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
        OnVideoSizeChangedListener, SurfaceHolder.Callback, MediaPlayer.OnErrorListener,
        MediaController.MediaPlayerControl {

    private static final String TAG = "VitamioMedia";
    private static final int DEFAULT_BG_COLOR = Color.BLACK;
    private static final String DEFAULT_BG_SCALE_TYPE = "fit";
    private Bundle extras;
    private MediaPlayer mMediaPlayer;
    private MediaController mMediaController = null;
    private SurfaceView mMediaView;
    private SurfaceHolder holder;
    private String path;
    private String mMediaType = null;
    private boolean mIsMediaSizeKnown = false;
    private boolean mIsMediaReadyToBePlayed = false;
    private ProgressBar mProgressBar = null;
    private boolean hasBackground = false;
    private Bitmap background = null;
    private ImageView.ScaleType bgImageScaleType = ImageView.ScaleType.CENTER;
    private ImageView bgImage = null;
    private int bgColor = DEFAULT_BG_COLOR;
    private boolean isStreaming = false;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
            return;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        extras = getIntent().getExtras();
        if (extras.containsKey("isStreaming")) {
            isStreaming = extras.getBoolean("isStreaming");
        }
        mMediaType = extras.getString("type");

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }

        mMediaPlayer = new MediaPlayer(this);
        mMediaController = new MediaController(this, !isStreaming);
        mMediaController.setMediaPlayer(this);
        mMediaPlayer.setOnBufferingUpdateListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnVideoSizeChangedListener(this);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        RelativeLayout relLayout = new RelativeLayout(this);

        if (extras.containsKey("bgColor")) {
            Log.v(TAG, "bgColor present - " + extras.getString("bgColor"));
            try {
                bgColor = Color.parseColor(extras.getString("bgColor"));
                Log.v(TAG, "Color parsed: " + bgColor);
            } catch (Exception e) {
                Log.v(TAG, "Error parsing color");
                Log.e(TAG, e.toString());
                bgColor = DEFAULT_BG_COLOR;
            }
        }
        relLayout.setBackgroundColor(bgColor);

        RelativeLayout.LayoutParams relLayoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
        relLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mMediaView = new SurfaceView(this);
        mMediaView.setLayoutParams(relLayoutParam);
        relLayout.addView(mMediaView);

        mProgressBar = new ProgressBar(this);
        mProgressBar.setIndeterminate(true);
        mProgressBar.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams pblp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        pblp.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
        mProgressBar.setLayoutParams(pblp);
        relLayout.addView(mProgressBar);
        mProgressBar.bringToFront();

        mMediaController.setAnchorView(relLayout);
        mMediaController.setEnabled(true);
        if (mMediaType.equalsIgnoreCase("audio")) {
            mMediaView.setBackgroundColor(bgColor);
            if (extras.containsKey("background")) {
                if (extras.containsKey("bgImageScaleType")) {
                    String scaleType = extras.getString("bgImageScaleType");
                    if (scaleType.equals("fit")) {
                        bgImageScaleType = ImageView.ScaleType.FIT_CENTER;
                    } else if (scaleType.equals("stretch")) {
                        bgImageScaleType = ImageView.ScaleType.FIT_XY;
                    } else {
                        bgImageScaleType = ImageView.ScaleType.CENTER;
                    }
                }
                bgImage = new ImageView(this);
                new ImageLoadTask(extras.getString("background"), this).execute(null, null);
                RelativeLayout.LayoutParams bgImageLayoutParam = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
                bgImageLayoutParam.addRule(RelativeLayout.CENTER_IN_PARENT);
                bgImage.setLayoutParams(bgImageLayoutParam);
                bgImage.setScaleType(bgImageScaleType);
                relLayout.addView(bgImage);
            }
        }
        setContentView(relLayout, relLayoutParam);
        holder = mMediaView.getHolder();
        holder.addCallback(this);
        holder.setFormat(PixelFormat.RGBA_8888);
    }

    @Override
    public void imageLoaded(String key, Bitmap bmp) {
        if (bgImage != null) {
            bgImage.setImageBitmap(bmp);
        }
    }

    public void onBufferingUpdate(MediaPlayer arg0, int percent) {
        Log.d(TAG, "onBufferingUpdate percent:" + percent);
    }

    public void onCompletion(MediaPlayer arg0) {
        Log.d(TAG, "onCompletion called");
    }

    public void onPrepared(MediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        mIsMediaReadyToBePlayed = true;
        if (mIsMediaReadyToBePlayed) {
            if (mMediaController != null)
                mMediaController.show();
            start();
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        releaseMediaPlayer();
        doCleanUp();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        doCleanUp();
    }

    private void releaseMediaPlayer() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    private void doCleanUp() {
        mIsMediaReadyToBePlayed = false;
    }

    public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
        Log.d(TAG, "surfaceChanged called");
        fixMediaSize();
    }

    public void surfaceDestroyed(SurfaceHolder surfaceholder) {
        Log.d(TAG, "surfaceDestroyed called");
        releaseMediaPlayer();
        doCleanUp();
    }

    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated called");
        Log.d(TAG, "background? " + extras.containsKey("background"));
        setupMedia();
    }

    private void setupMedia() {
        doCleanUp();
        try {
            mMediaPlayer.setDataSource(extras.getString("mediaUrl"));
            mMediaPlayer.setDisplay(holder);
            mMediaPlayer.prepareAsync();
        } catch (Exception e) {
            Log.e(TAG, "error: " + e.getMessage(), e);
        }
    }

    public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
        Log.v(TAG, "onVideoSizeChanged called");
        if (width == 0 || height == 0) {
            Log.e(TAG, "invalid video width(" + width + ") or height(" + height + ")");
            return;
        }
        mIsMediaSizeKnown = true;
        if (mIsMediaReadyToBePlayed && mIsMediaSizeKnown) {
            fixMediaSize();
        }
    }

    private void fixMediaSize() {
        int width = mMediaPlayer.getVideoWidth();
        int height = mMediaPlayer.getVideoHeight();
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        android.view.ViewGroup.LayoutParams videoParams = mMediaView.getLayoutParams();

        if (width > 0 && height > 0) {
            Log.i(TAG, "Video is: " + width + " x " + height);

            if (width > height) {
                videoParams.width = screenWidth;
                videoParams.height = screenWidth * height / width;
            } else {
                videoParams.width = screenHeight * width / height;
                videoParams.height = screenHeight;
            }
        } else {
            videoParams.width = 0;
            videoParams.height = 0;
        }

        Log.i(TAG, "Setting dimensions to: " + videoParams.width + " x " + videoParams.height);
        mMediaView.setLayoutParams(videoParams);
    }


    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        StringBuilder sb = new StringBuilder();
        sb.append("Media Player Error: ");
        switch (what) {
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                sb.append("Not Valid for Progressive Playback");
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                sb.append("Unknown");
                break;
            default:
                sb.append(" Non standard (");
                sb.append(what);
                sb.append(")");
        }
        sb.append(" (" + what + ") ");
        sb.append(extra);
        Log.e(TAG, sb.toString());
        finish();
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // The screen size changed or the orientation changed... don't restart the activity
        super.onConfigurationChanged(newConfig);
        fixMediaSize();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.v(TAG, "SCREEN TOUCHED");
        if (mIsMediaReadyToBePlayed) {
            Log.v(TAG, "NOT NULL");
            mMediaController.show();
        }
        Log.v(TAG, "Is Controller showing? " + mMediaController.isShowing());
        return false;
    }

    @Override
    public void start() {
        mMediaPlayer.start();
    }

    @Override
    public void pause() {
        mMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return (int) mMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return (int) mMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int i) {
        mMediaPlayer.seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
