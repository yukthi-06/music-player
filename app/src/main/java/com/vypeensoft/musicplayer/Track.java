package com.vypeensoft.musicplayer;

/**
 * Represents a single MP3 audio track.
 */
public class Track {
    private final String path;
    private final String title;
    private final String folderPath;

    public Track(String path, String title, String folderPath) {
        this.path = path;
        this.title = title;
        this.folderPath = folderPath;
    }

    public String getPath() {
        return path;
    }

    public String getTitle() {
        return title;
    }

    public String getFolderPath() {
        return folderPath;
    }

    @Override
    public String toString() {
        return title;
    }
}
