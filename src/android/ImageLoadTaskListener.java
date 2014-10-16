package com.hutchind.cordova.plugins.vitamio;

import android.graphics.Bitmap;

/**
 * Created by nchutchind on 10/10/2014.
 */
public interface ImageLoadTaskListener {
    public void imageLoaded(String key, Bitmap bmp);
}
