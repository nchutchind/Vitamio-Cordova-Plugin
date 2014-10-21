"use strict";
function Vitamio() {
}

Vitamio.prototype.playAudio = function (url, options) {
	options = options || {};
	var handleSuccessCallback = function(playbackInfo) {
		if (options.successCallback && playbackInfo.isDone) {
			options.successCallback(playbackInfo);
		} else if (options.progressCallback && !playbackInfo.isDone) {
			options.progressCallback(playbackInfo);
		}
	}
	cordova.exec(handleSuccessCallback, options.errorCallback || null, "Vitamio", "playAudio", [url, options]);
};
Vitamio.prototype.playVideo = function (url, options) {
	options = options || {};
	var handleSuccessCallback = function(playbackInfo) {
		if (options.successCallback && playbackInfo.isDone) {
			options.successCallback(playbackInfo);
		} else if (options.progressCallback && !playbackInfo.isDone) {
			options.progressCallback(playbackInfo);
		}
	}
	cordova.exec(handleSuccessCallback, options.errorCallback || null, "Vitamio", "playVideo", [url, options]);
};


Vitamio.install = function () {
	if (!window.plugins) {
		window.plugins = {};
	}
	window.plugins.vitamio = new Vitamio();
	return window.plugins.vitamio;
};

cordova.addConstructor(Vitamio.install);