package com.vypeensoft.musicplayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a playlist (can be mock or real).
 */
public class Playlist {
    private final String name;
    private final List<Track> tracks;

    public Playlist(String name) {
        this.name = name;
        this.tracks = new ArrayList<>();
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
