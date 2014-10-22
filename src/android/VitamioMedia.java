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
import android.content.Intent;

public class VitamioMedia extends Activity implements ImageLoadTaskListener,
        OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener,
        OnVideoSizeChangedListener, SurfaceHolder.Callback, MediaPlayer.OnErrorListener,
        MediaController.MediaPlayerControl {

    public static final String ACTION_INFO = "com.hutchind.cordova.plugins.vitamio-broadcastVitamioMediaInfo";
    private static final String TAG = "VitamioMedia";
    private static final int DEFAULT_BG_COLOR = Color.BLACK;
    private static final String DEFAULT_BG_SCALE_TYPE = "fit";
    private static final String MEDIA_TYPE_VIDEO = "video";
    private static final String MEDIA_TYPE_AUDIO = "audio";
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
    private Boolean isStreaming = false;
    private Boolean mShouldAutoClose = true;
    private int lastKnownPosition = -1;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
            return;

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        extras = getIntent().getExtras();

        // handle extras
        if (extras == null) {
            wrapItUp(RESULT_CANCELED, "Error: No options provided");
        } else {
            if (extras.containsKey("isStreaming")) {
               isStreaming = extras.getBoolean("isStreaming");
            }

            if (extras.containsKey("shouldAutoClose")) {
                mShouldAutoClose = extras.getBoolean("shouldAutoClose");
            }

            mMediaType = extras.getString("type");
            if (mMediaType == null) mMediaType = MEDIA_TYPE_VIDEO;

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
                try {
                    bgColor = Color.parseColor(extras.getString("bgColor"));
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
            if (mMediaType.equalsIgnoreCase(MEDIA_TYPE_AUDIO)) {
                mMediaView.setBackgroundColor(bgColor);
                if (extras.containsKey("bgImage")) {
                    if (extras.containsKey("bgImageScaleType")) {
                        String scaleType = extras.getString("bgImageScaleType");
                        if (scaleType.equalsIgnoreCase("fit")) {
                            bgImageScaleType = ImageView.ScaleType.FIT_CENTER;
                        } else if (scaleType.equalsIgnoreCase("stretch")) {
                            bgImageScaleType = ImageView.ScaleType.FIT_XY;
                        } else {
                            bgImageScaleType = ImageView.ScaleType.CENTER;
                        }
                    }
                    bgImage = new ImageView(this);
                    new ImageLoadTask(extras.getString("bgImage"), this).execute(null, null);
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
    }

    @Override
    public void imageLoaded(String key, Bitmap bmp) {
        if (bgImage != null) {
            bgImage.setImageBitmap(bmp);
        }
    }

    public void onBufferingUpdate(MediaPlayer arg0, int percent) {
        //Log.d(TAG, "onBufferingUpdate percent:" + percent);
    }

    public void onCompletion(MediaPlayer mp) {
        Log.d(TAG, "onCompletion called");
        if (mShouldAutoClose) {
            wrapItUp(RESULT_OK, null);
        }
    }

    public void onPrepared(MediaPlayer mediaplayer) {
        Log.d(TAG, "onPrepared called");
        mIsMediaReadyToBePlayed = true;
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            start();
            if (mMediaController != null)
                mMediaController.show();
            mProgressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        Log.v(TAG,"onDestroy");
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
        if (mMediaPlayer == null || !mIsMediaReadyToBePlayed)
            return;
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

        wrapItUp(RESULT_CANCELED, sb.toString());

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
        if (mIsMediaReadyToBePlayed) {
            mMediaController.show();
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        wrapItUp(RESULT_OK, null);
    }

    private void wrapItUp(int resultCode, String message) {
        Intent intent = new Intent();
        if (message != null) {
            intent.putExtra("message", message);
        }
        if (!isStreaming) {
            try {
                intent.putExtra("pos", getCurrentPosition());
            } catch (Exception e) {
                intent.putExtra("pos", -1);
            }
        }
        setResult(resultCode, intent);
        finish();
    }

    @Override
    public void start() {
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            mMediaPlayer.start();
            Intent intent = new Intent();
            intent.setAction(ACTION_INFO);
            intent.putExtra("action", "start");
            if (!isStreaming) intent.putExtra("pos", getCurrentPosition());
            sendBroadcast(intent);
        } else {
            Log.e(TAG, "MediaPlayer is not instantiated yet.");
        }
    }

    public void stop() {
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            mMediaPlayer.stop();
            Intent intent = new Intent();
            intent.setAction(ACTION_INFO);
            intent.putExtra("action", "stop");
            if (!isStreaming) intent.putExtra("pos", getCurrentPosition());
            sendBroadcast(intent);
        } else {
            Log.e(TAG, "MediaPlayer is not instantiated yet.");
        }
    }

    @Override
    public void pause() {
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            mMediaPlayer.pause();
            Intent intent = new Intent();
            intent.setAction(ACTION_INFO);
            intent.putExtra("action", "pause");
            if (!isStreaming) intent.putExtra("pos", getCurrentPosition());
            sendBroadcast(intent);
        } else {
            Log.e(TAG, "MediaPlayer is not instantiated yet.");
        }
    }

    @Override
    public int getDuration() {
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            return (int) mMediaPlayer.getDuration();
        } else {
            return -1;
        }
    }

    @Override
    public int getCurrentPosition() {
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            return (int) mMediaPlayer.getCurrentPosition();
        } else {
            return -1;
        }
    }

    @Override
    public void seekTo(int i) {
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            mMediaPlayer.seekTo(i);
        } else {
            Log.e(TAG, "MediaPlayer is not instantiated yet.");
        }
    }

    @Override
    public boolean isPlaying() {
        if (mMediaPlayer != null && mIsMediaReadyToBePlayed) {
            return mMediaPlayer.isPlaying();
        } else {
            return false;
        }
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
        return !isStreaming;
    }

    @Override
    public boolean canSeekForward() {
        return !isStreaming;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}
