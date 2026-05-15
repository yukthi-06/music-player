package com.vypeensoft.musicplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a folder containing one or more tracks.
 */
public class Folder {
    private final String path;
    private final String name;
    private final List<Track> tracks;

    public Folder(String path, String name) {
        this.path = path;
        this.name = name;
        this.tracks = new ArrayList<>();
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public List<Track> getTracks() {
        return tracks;
    }

    public void addTrack(Track track) {
        tracks.add(track);
    }

    @Override
    public String toString() {
        return name;
    }
}
