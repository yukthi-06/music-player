package com.vypeensoft.musicplayer;

import android.media.MediaPlayer;
import android.util.Log;
import java.io.IOException;

/**
 * Manages music playback using MediaPlayer.
 */
public class PlaybackManager {
    private static final String TAG = "PlaybackManager";
    private MediaPlayer mediaPlayer;
    private final PlaybackListener listener;
    private String currentPath;

    public interface PlaybackListener {
        void onTrackFinished();
        void onError(String error);
    }

    public PlaybackManager(PlaybackListener listener) {
        this.listener = listener;
        initMediaPlayer();
    }

    private void initMediaPlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(mp -> listener.onTrackFinished());
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            listener.onError("MediaPlayer error: " + what);
            return false;
        });
    }

    public void play(String path) {
        try {
            if (path.equals(currentPath) && isPlaying()) {
                // Already playing this
                return;
            }

            mediaPlayer.reset();
            mediaPlayer.setDataSource(path);
            mediaPlayer.prepare();
            mediaPlayer.start();
            currentPath = path;
        } catch (IOException e) {
            Log.e(TAG, "Error playing track", e);
            listener.onError("Could not play file: " + e.getMessage());
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void seekTo(int msec) {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(msec);
        }
    }

    public int getDuration() {
        if (mediaPlayer != null && (currentPath != null)) {
            return mediaPlayer.getDuration();
        }
        return 0;
    }

    public int getCurrentPosition() {
        if (mediaPlayer != null) {
            return mediaPlayer.getCurrentPosition();
        }
        return 0;
    }

    public void setVolume(float volume) {
        if (mediaPlayer != null) {
            mediaPlayer.setVolume(volume, volume);
        }
    }

    public void resume() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
        }
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public void release() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
