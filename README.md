# Cordova Vitamio plugin 

for Android, by [Nicholas Hutchind](https://github.com/nchutchind)

1. [Description](https://github.com/nchutchind/Vitamio-Cordova-Plugin#1-description)
2. [Usage](https://github.com/nchutchind/Vitamio-Cordova-Plugin#2-usage)

## 1. Description

This plugin allows you to stream audio and video in a fullscreen, native player using Vitamio software decoding on Android.

* Works with PhoneGap >= 3.0.

### Android specifics
* Uses Vitamio and FFMPEG.
* Uses Vitamio's Online version of FFMPEG (4.6MB). Other versions of FFMPEG may be able to be dropped into the res/raw folder to replace the libarm.so file (your mileage may vary).
* Uses SurfaceView. Allows you to display an image and color the background for audio.
* Creates an activiy in your AndroidManifest.xml file.
* Tested on Android 4.0+. (If someone has a Gingerbread device, please report back to me how it does.)

## 2. Usage

```javascript
  var videoUrl = VIDEO_URL;

  // Just play a video
  window.plugins.vitamio.playVideo(videoUrl);
  
  // Play a pre-recorded video with callbacks
  var options = {
    isStreaming: false,
    successCallback: function(obj) {
      console.log("Video was closed without error at " + obj.pos + ".");
    },
    progressCallback: function(obj) {
      // obj.action may be "start", "stop", or "pause"
      // obj.pos is the current time in the clip (only available for non-continuous streams)
      //   and shows a format of "00:12:34"
      myAnalytics.track(obj.action, obj.pos);
    },
    errorCallback: function(obj) {
      console.log("Error! " + obj.message);
    }
  };
  window.plugins.vitamio.playVideo(videoUrl, options);

  var audioUrl = STREAMING_AUDIO_URL;
  
  // Simply play an audio file (not recommended, since the screen will be plain black)
  window.plugins.vitamio.playAudio(audioUrl);

  // Play an audio file with options (all options optional)
  var options = {
    isStreaming: true,
    bgColor: "#FFFFFF",
    bgImage: "<SWEET_BACKGROUND_IMAGE>",
    bgImageScale: "fit",
    successCallback: function() {
      console.log("Player closed without error.");
    },
    errorCallback: function(obj) {
      console.log("Error! " + obj.message);
    }
  };
  window.plugins.vitamio.playAudio(audioUrl, options);
```

##3. Options

### Common Options
* isStreaming - (optional) Determines whether the fast-forward and rewind buttons are displayed in the control bar or not, as well as whether the current position in the media is reported. If true, the buttons will be removed and the current position will not be reported (or reported as "00:00:00"). Defaults to *false*.
* shouldAutoClose - (optional) Whether or not the media activity will automatically close when the media is finished playing. Has no effect on continuous streaming media. Defaults to *true*.
* successCallback - (optional) function to call once the media has finished playing, or the user has backed out of the activity. Receives an object containing the position ("pos") the media was at when it stopped, if "isStreaming" is *false*.
* progressCallback - (optional) function to call when the media is started, stopped, or paused. Receives an object containing the "action" that happened ("start", "stop" or "pause") and the current position ("pos") the media was at when the action occurred, if "isStreaming" is *false*.
* errorCallback - (optional) function to call if there is an error while playing the media. Receives an object containing the error "message", as well as the current position ("pos") the media was at when the action occurred, if "isStreaming" is *false*.

### Video-Only Options
There are no video-only options.

### Audio-Only Options
* bgColor - (optional) HEX value for the background color of the screen. Defaults to "#000000"
* bgImage - (optional) URL for an image to display on the screen while audio plays. No default.
* bgImageScale - (optional) Scaling technique to use for the bgImage. May be one of the following:
  * fit - (default) fits the image on the screen, enlarging it while respecting the image aspect ratio
  * stretch - stretches the image so it fills all available screen space, disregarding the image aspect ratio
  * center - centers the image on the screen, keeping it at the original image size
