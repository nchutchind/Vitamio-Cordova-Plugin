package com.hutchind.cordova.plugins.vitamio;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

public class Vitamio extends CordovaPlugin {
	public static final String ACTION_PLAY_AUDIO = "playAudio";
	public static final String ACTION_PLAY_VIDEO = "playVideo";

	private static final int ACTIVITY_CODE_PLAY_MEDIA = 7;

	private CallbackContext callbackContext;
	private BroadcastReceiver receiver;

	private static final String TAG = "VitamioPlugin";

	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		this.callbackContext = callbackContext;
		JSONObject options = null;

		try {
			options = args.getJSONObject(1);
		} catch (JSONException e) {
			// Developer provided no options. Leave options null.
		}

		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(VitamioMedia.ACTION_INFO);
		if (this.receiver == null) {
			this.receiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					updateMediaInfo(intent);
				}
			};
			cordova.getActivity().registerReceiver(this.receiver, intentFilter);
		}

		if (ACTION_PLAY_AUDIO.equals(action)) {
			return playAudio(args.getString(0), options);
		} else if (ACTION_PLAY_VIDEO.equals(action)) {
			return playVideo(args.getString(0), options);
		} else {
			callbackContext.error("vitamio." + action + " is not a supported method.");
			return false;
		}
	}

	public void onDestroy() {
		removeMediaListener();
	}

	public void onReset() {
		removeMediaListener();
	}

	private void removeMediaListener() {
		if (this.receiver != null) {
			try {
				this.cordova.getActivity().unregisterReceiver(this.receiver);
				this.receiver = null;
			} catch (Exception e) {
				Log.e(TAG, "Error unregistering media receiver: " + e.getMessage(), e);
			}
		}
	}
	private void updateMediaInfo(Intent mediaIntent) {
		sendUpdate(this.getMediaInfo(mediaIntent), true);
	}

	private void sendUpdate(JSONObject info, boolean keepCallback) {
		if (callbackContext != null) {
			PluginResult result = new PluginResult(PluginResult.Status.OK, info);
			result.setKeepCallback(keepCallback);
			callbackContext.sendPluginResult(result);
		}
	}

	private JSONObject getMediaInfo(Intent mediaIntent) {
		JSONObject obj = new JSONObject();
		try {
			obj.put("action", mediaIntent.getStringExtra("action"));
			if (mediaIntent.hasExtra("pos")) {
				obj.put("pos", getTimeString(mediaIntent.getIntExtra("pos", -1)));
			}
			obj.put("isDone", false);
		} catch (JSONException e) {
			Log.e(TAG, e.getMessage(), e);
		}
		return obj;
	}

	private boolean playAudio(String url, JSONObject options) throws JSONException {
		options.put("type", "audio");
		return play(VitamioMedia.class, url, options);
	}
	private boolean playVideo(String url, JSONObject options) throws JSONException {
		options.put("type", "video");
		return play(VitamioMedia.class, url, options);
	}

	private boolean play(final Class activityClass, final String url, final JSONObject options) {
		final CordovaInterface cordovaObj = cordova;
		final CordovaPlugin plugin = this;

		cordova.getActivity().runOnUiThread(new Runnable() {
			public void run() {
				final Intent streamIntent = new Intent(cordovaObj.getActivity().getApplicationContext(), activityClass);
				Bundle extras = new Bundle();
				extras.putString("mediaUrl", url);

				if (options != null) {
					Iterator<String> optKeys = options.keys();
					while (optKeys.hasNext()) {
						try {
							final String optKey = (String)optKeys.next();
							if (options.get(optKey).getClass().equals(String.class)) {
								extras.putString(optKey, (String)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							} else if (options.get(optKey).getClass().equals(Boolean.class)) {
								extras.putBoolean(optKey, (Boolean)options.get(optKey));
								Log.v(TAG, "Added option: " + optKey + " -> " + String.valueOf(options.get(optKey)));
							}

						} catch (JSONException e) {
							Log.e(TAG, "JSONException while trying to read options. Skipping option.");
						}
					}
					streamIntent.putExtras(extras);
				}

				cordovaObj.startActivityForResult(plugin, streamIntent, ACTIVITY_CODE_PLAY_MEDIA);
			}
		});
		return true;
	}

	private String getTimeString(int millis) {
		if (millis == -1)
			return "00:00:00";
		StringBuffer buf = new StringBuffer();

		int hours = (int) (millis / (1000 * 60 * 60));
		int minutes = (int) ((millis % (1000 * 60 * 60)) / (1000 * 60));
		int seconds = (int) (((millis % (1000 * 60 * 60)) % (1000 * 60)) / 1000);

		buf
			.append(String.format("%02d", hours))
			.append(":")
			.append(String.format("%02d", minutes))
			.append(":")
			.append(String.format("%02d", seconds));

		return buf.toString();
	}

	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		Log.v(TAG, "onActivityResult: " + requestCode + " " + resultCode);
		super.onActivityResult(requestCode, resultCode, intent);
		if (ACTIVITY_CODE_PLAY_MEDIA == requestCode) {
			JSONObject obj = new JSONObject();
			if (Activity.RESULT_OK == resultCode) {
				try {
					obj.put("isDone", true);
					if (intent.hasExtra("pos")) {
						obj.put("pos", getTimeString(intent.getIntExtra("pos", -1)));
					}
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				this.callbackContext.success(obj);
			} else if (Activity.RESULT_CANCELED == resultCode) {
				String errMsg = "Error";
				try {
					if (intent != null) {
						if (intent.hasExtra("message")) {
							obj.put("message", intent.getStringExtra("message"));
						}
						if (intent.hasExtra("pos")) {
							obj.put("pos", getTimeString(intent.getIntExtra("pos", -1)));
						}
					}
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage(), e);
				}
				this.callbackContext.error(obj);
			}
		}
	}
}
