package com.armedarms.idealmedia.domain;

public interface IPlayerController {
	public void onPluginsLoaded(String plugins);
	public void onFileLoaded(Track track, double duration, String artist, String title, int position, int albumId);
	public void onProgressChanged(int position, double progress, double duration);
    public void onUpdatePlayPause();
}
