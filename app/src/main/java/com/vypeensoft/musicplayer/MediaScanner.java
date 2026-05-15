package com.vypeensoft.musicplayer;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scans the device storage for MP3 files using MediaStore.
 */
public class MediaScanner {

    public interface ScanCallback {
        void onScanComplete(List<Folder> folders, List<Playlist> playlists);
    }

    public static void scanMedia(Context context, ScanCallback callback) {
        new Thread(() -> {
            android.content.SharedPreferences prefs = context.getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE);
            String savedPaths = prefs.getString("scan_folders", "");
            String[] rawPaths = savedPaths.split("\n");
            final java.util.List<String> allowedPaths = new java.util.ArrayList<>();
            for (String p : rawPaths) {
                if (!p.trim().isEmpty()) {
                    allowedPaths.add(p.trim());
                }
            }

            ContentResolver contentResolver = context.getContentResolver();
            Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
            String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

            Cursor cursor = contentResolver.query(uri, null, selection, null, sortOrder);
            Map<String, Folder> folderMap = new HashMap<>();

            if (cursor != null && cursor.moveToFirst()) {
                int titleColumn = cursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
                int pathColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DATA);

                do {
                    String thisTitle = cursor.getString(titleColumn);
                    String thisPath = cursor.getString(pathColumn);

                    boolean isAllowed = true;
                    if (!allowedPaths.isEmpty()) {
                        isAllowed = false;
                        for (String allowedPath : allowedPaths) {
                            if (thisPath.startsWith(allowedPath)) {
                                isAllowed = true;
                                break;
                            }
                        }
                    }

                    if (!isAllowed) {
                        continue;
                    }

                    File file = new File(thisPath);
                    String folderPath = file.getParent();
                    String folderName = "Unknown";
                    if (folderPath != null) {
                        folderName = new File(folderPath).getName();
                    }

                    Track track = new Track(thisPath, thisTitle, folderPath);

                    Folder folder = folderMap.get(folderPath);
                    if (folder == null) {
                        folder = new Folder(folderPath, folderName);
                        folderMap.put(folderPath, folder);
                    }
                    folder.addTrack(track);

                } while (cursor.moveToNext());
                cursor.close();
            }

            List<Folder> folderList = new ArrayList<>(folderMap.values());
            // Sort folders by name
            Collections.sort(folderList, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            List<Playlist> playlistList = new ArrayList<>();
            Uri filesUri = MediaStore.Files.getContentUri("external");
            String selectionPl = MediaStore.Files.FileColumns.DATA + " LIKE '%.m3u' OR " +
                                 MediaStore.Files.FileColumns.DATA + " LIKE '%.pls' OR " +
                                 MediaStore.Files.FileColumns.DATA + " LIKE '%.m3u8'";
            Cursor plCursor = null;
            try {
                plCursor = contentResolver.query(filesUri, null, selectionPl, null, null);
                if (plCursor != null && plCursor.moveToFirst()) {
                    int pathColumnPl = plCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA);
                    int titleColumnPl = plCursor.getColumnIndex(MediaStore.Files.FileColumns.TITLE);
                    if (titleColumnPl == -1) {
                        titleColumnPl = plCursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME);
                    }

                    do {
                        if (pathColumnPl == -1) continue;
                        String path = plCursor.getString(pathColumnPl);
                        if (path == null) continue;
                        
                        String title = "Unknown";
                        if (titleColumnPl != -1) {
                            title = plCursor.getString(titleColumnPl);
                        }
                        if (title == null || title.equals("Unknown")) {
                             File f = new File(path);
                             title = f.getName();
                        }

                        Playlist playlist = new Playlist(title);
                        parsePlaylist(path, playlist);
                        playlistList.add(playlist);

                    } while (plCursor.moveToNext());
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (plCursor != null) {
                    plCursor.close();
                }
            }

            // Sort playlists by name
            Collections.sort(playlistList, (p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));

            callback.onScanComplete(folderList, playlistList);
        }).start();
    }

    private static void parsePlaylist(String path, Playlist playlist) {
        try {
            java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.FileReader(path));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.toLowerCase().startsWith("file") && line.contains("=")) {
                    // PLS format: File1=path
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2) {
                        line = parts[1].trim();
                    }
                }
                
                File trackFile = new File(line);
                if (!trackFile.isAbsolute()) {
                    File plFile = new File(path);
                    trackFile = new File(plFile.getParent(), line);
                }
                
                if (trackFile.exists()) {
                    playlist.addTrack(new Track(trackFile.getAbsolutePath(), trackFile.getName(), trackFile.getParent()));
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
