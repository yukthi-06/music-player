package com.vypeensoft.musicplayer;

import android.Manifest;
import android.content.Intent;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import java.util.Collections;
import java.util.Random;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int PERMISSION_REQUEST_CODE = 100;

    // UI Components
    private DrawerLayout drawerLayout;
    private View barFolder, barFile, barPlaylist, barGrandParent;
    private TextView txtFolder, txtFile, txtPlaylist, txtGrandParent, txtNowPlaying;
    
    // Playback and Data
    private PlaybackManager playbackManager;
    private List<Folder> folders = new ArrayList<>();
    private List<Playlist> playlists = new ArrayList<>();
    private List<String> grandParents = new ArrayList<>();
    
    private int currentFolderIndex = 0;
    private int currentFileIndex = 0;
    private int currentPlaylistIndex = 0;
    private int currentGrandParentIndex = -1;
    
    // Selection state: 0=Folder, 1=File, 2=Playlist, 3=GrandParent
    private int activeBarIndex = 0;

    // New UI Components for Controls
    private ImageButton btnPlayPause, btnShuffle, btnLoop;
    private SeekBar seekProgress, seekVolume;
    private TextView txtCurrentTime, txtTotalTime;

    // Playback State
    private boolean isShuffleEnabled = false;
    private boolean isLoopOneEnabled = false;
    private Handler progressHandler = new Handler(Looper.getMainLooper());
    private Runnable progressUpdater;
    private List<Track> shuffledTracks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        setupNavigationDrawer();
        initViews();
        setupGestureDetectors();
        setupButtonListeners();

        playbackManager = new PlaybackManager(new PlaybackManager.PlaybackListener() {
            @Override
            public void onTrackFinished() {
                playNextAuto();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
            }
        });

        checkPermissions();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupNavigationDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        Toolbar toolbar = findViewById(R.id.toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar, R.string.bar_folder, R.string.bar_folder);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    private void initViews() {
        barFolder = findViewById(R.id.bar_folder_container);
        barFile = findViewById(R.id.bar_file_container);
        barPlaylist = findViewById(R.id.bar_playlist_container);
        barGrandParent = findViewById(R.id.bar_grand_parent_container);

        txtFolder = findViewById(R.id.txt_folder_name);
        txtFile = findViewById(R.id.txt_file_name);
        txtPlaylist = findViewById(R.id.txt_playlist_name);
        txtGrandParent = findViewById(R.id.txt_grand_parent_name);
        txtNowPlaying = findViewById(R.id.txt_now_playing);

        // Control Panel Views
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnShuffle = findViewById(R.id.btn_shuffle);
        btnLoop = findViewById(R.id.btn_loop);
        seekProgress = findViewById(R.id.seekbar_progress);
        seekVolume = findViewById(R.id.seekbar_volume);
        txtCurrentTime = findViewById(R.id.txt_current_time);
        txtTotalTime = findViewById(R.id.txt_total_time);

        setupControlListeners();
        setupProgressUpdater();

        // Set initial selection
        updateActiveBar(0);
    }

    private void setupGestureDetectors() {
        // Folder Bar Gestures
        GestureDetector folderDetector = new GestureDetector(this, new GestureListener(new GestureListener.SwipeCallback() {
            @Override
            public void onSwipeLeft() { nextFolder(); }
            @Override
            public void onSwipeRight() { prevFolder(); }
        }));
        barFolder.setOnTouchListener((v, event) -> {
            setActiveBar(0);
            return folderDetector.onTouchEvent(event);
        });

        // File Bar Gestures
        GestureDetector fileDetector = new GestureDetector(this, new GestureListener(new GestureListener.SwipeCallback() {
            @Override
            public void onSwipeLeft() { nextFile(); }
            @Override
            public void onSwipeRight() { prevFile(); }
        }));
        barFile.setOnTouchListener((v, event) -> {
            setActiveBar(1);
            return fileDetector.onTouchEvent(event);
        });

        // Playlist Bar Gestures
        GestureDetector playlistDetector = new GestureDetector(this, new GestureListener(new GestureListener.SwipeCallback() {
            @Override
            public void onSwipeLeft() { nextPlaylist(); }
            @Override
            public void onSwipeRight() { prevPlaylist(); }
        }));
        barPlaylist.setOnTouchListener((v, event) -> {
            setActiveBar(2);
            return playlistDetector.onTouchEvent(event);
        });

        // Grand Parent Bar Gestures
        GestureDetector grandParentDetector = new GestureDetector(this, new GestureListener(new GestureListener.SwipeCallback() {
            @Override
            public void onSwipeLeft() { nextGrandParent(); }
            @Override
            public void onSwipeRight() { prevGrandParent(); }
        }));
        barGrandParent.setOnTouchListener((v, event) -> {
            setActiveBar(3);
            return grandParentDetector.onTouchEvent(event);
        });

        // Clicks for play selection
        barFolder.setOnClickListener(v -> playCurrentFolder());
        barFile.setOnClickListener(v -> playCurrentFile());
        barPlaylist.setOnClickListener(v -> playCurrentPlaylist());
        barGrandParent.setOnClickListener(v -> playCurrentFolder());
    }

    private void setupButtonListeners() {
        findViewById(R.id.btn_folder_left).setOnClickListener(v -> prevFolder());
        findViewById(R.id.btn_folder_right).setOnClickListener(v -> nextFolder());
        
        findViewById(R.id.btn_file_left).setOnClickListener(v -> prevFile());
        findViewById(R.id.btn_file_right).setOnClickListener(v -> nextFile());
        
        findViewById(R.id.btn_playlist_left).setOnClickListener(v -> prevPlaylist());
        findViewById(R.id.btn_playlist_right).setOnClickListener(v -> nextPlaylist());

        findViewById(R.id.btn_grand_parent_left).setOnClickListener(v -> prevGrandParent());
        findViewById(R.id.btn_grand_parent_right).setOnClickListener(v -> nextGrandParent());
    }

    private void setupControlListeners() {
        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        findViewById(R.id.btn_next).setOnClickListener(v -> nextFile());
        findViewById(R.id.btn_prev).setOnClickListener(v -> prevFile());
        
        btnShuffle.setOnClickListener(v -> toggleShuffle());
        btnLoop.setOnClickListener(v -> toggleLoop());

        seekProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    txtCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                progressHandler.removeCallbacks(progressUpdater);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                playbackManager.seekTo(seekBar.getProgress());
                if (playbackManager.isPlaying()) {
                    progressHandler.postDelayed(progressUpdater, 1000);
                }
            }
        });

        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                playbackManager.setVolume(progress / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupProgressUpdater() {
        progressUpdater = new Runnable() {
            @Override
            public void run() {
                if (playbackManager != null && playbackManager.isPlaying()) {
                    int currentPos = playbackManager.getCurrentPosition();
                    seekProgress.setProgress(currentPos);
                    txtCurrentTime.setText(formatTime(currentPos));
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
    }

    private void setActiveBar(int index) {
        if (activeBarIndex != index) {
            updateActiveBar(index);
        }
    }

    private void updateActiveBar(int index) {
        activeBarIndex = index;
        barFolder.setSelected(index == 0);
        barFile.setSelected(index == 1);
        barPlaylist.setSelected(index == 2);
        barGrandParent.setSelected(index == 3);
    }

    // --- Navigation Logic ---

    private void nextFolder() {
        if (folders.isEmpty()) return;
        String currentGpPath = getGrandParentPath(currentFolderIndex);
        if (currentGpPath == null) {
            currentFolderIndex = (currentFolderIndex + 1) % folders.size();
        } else {
            java.util.List<Integer> siblings = getSiblingIndices(currentGpPath);
            int subIndex = siblings.indexOf(currentFolderIndex);
            if (subIndex != -1) {
                int nextSub = (subIndex + 1) % siblings.size();
                currentFolderIndex = siblings.get(nextSub);
            } else {
                currentFolderIndex = (currentFolderIndex + 1) % folders.size();
            }
        }
        currentFileIndex = 0;
        updateUI();
        playCurrentFolder();
    }

    private void prevFolder() {
        if (folders.isEmpty()) return;
        String currentGpPath = getGrandParentPath(currentFolderIndex);
        if (currentGpPath == null) {
            currentFolderIndex = (currentFolderIndex - 1 + folders.size()) % folders.size();
        } else {
            java.util.List<Integer> siblings = getSiblingIndices(currentGpPath);
            int subIndex = siblings.indexOf(currentFolderIndex);
            if (subIndex != -1) {
                int prevSub = (subIndex - 1 + siblings.size()) % siblings.size();
                currentFolderIndex = siblings.get(prevSub);
            } else {
                currentFolderIndex = (currentFolderIndex - 1 + folders.size()) % folders.size();
            }
        }
        currentFileIndex = 0;
        updateUI();
        playCurrentFolder();
    }

    private void nextFile() {
        if (folders.isEmpty()) return;
        if (isShuffleEnabled && !shuffledTracks.isEmpty()) {
            currentFileIndex = (currentFileIndex + 1) % shuffledTracks.size();
            playTrack(shuffledTracks.get(currentFileIndex));
        } else {
            Folder currentFolder = folders.get(currentFolderIndex);
            if (currentFileIndex < currentFolder.getTracks().size() - 1) {
                currentFileIndex++;
            } else {
                String gpPath = getGrandParentPath(currentFolderIndex);
                if (gpPath != null) {
                    java.util.List<Integer> siblings = getSiblingIndices(gpPath);
                    int subIndex = siblings.indexOf(currentFolderIndex);
                    if (subIndex != -1) {
                        int nextSub = (subIndex + 1) % siblings.size();
                        currentFolderIndex = siblings.get(nextSub);
                    } else {
                        currentFolderIndex = (currentFolderIndex + 1) % folders.size();
                    }
                } else {
                    currentFolderIndex = (currentFolderIndex + 1) % folders.size();
                }
                currentFileIndex = 0;
            }
            updateUI();
            playCurrentFile();
        }
    }

    private void prevFile() {
        if (folders.isEmpty()) return;
        if (isShuffleEnabled && !shuffledTracks.isEmpty()) {
            currentFileIndex = (currentFileIndex - 1 + shuffledTracks.size()) % shuffledTracks.size();
            playTrack(shuffledTracks.get(currentFileIndex));
        } else {
            if (currentFileIndex > 0) {
                currentFileIndex--;
            } else {
                String gpPath = getGrandParentPath(currentFolderIndex);
                if (gpPath != null) {
                    java.util.List<Integer> siblings = getSiblingIndices(gpPath);
                    int subIndex = siblings.indexOf(currentFolderIndex);
                    if (subIndex != -1) {
                        int prevSub = (subIndex - 1 + siblings.size()) % siblings.size();
                        currentFolderIndex = siblings.get(prevSub);
                    } else {
                        currentFolderIndex = (currentFolderIndex - 1 + folders.size()) % folders.size();
                    }
                } else {
                    currentFolderIndex = (currentFolderIndex - 1 + folders.size()) % folders.size();
                }
                currentFileIndex = folders.get(currentFolderIndex).getTracks().size() - 1;
            }
            updateUI();
            playCurrentFile();
        }
    }

    private String getGrandParentPath(int folderIdx) {
        if (folderIdx < 0 || folderIdx >= folders.size()) return null;
        Folder f = folders.get(folderIdx);
        String path = f.getPath();
        if (path != null) {
            java.io.File parentFile = new java.io.File(path).getParentFile();
            if (parentFile != null) {
                return parentFile.getAbsolutePath();
            }
        }
        return null;
    }

    private java.util.List<Integer> getSiblingIndices(String gpPath) {
        java.util.List<Integer> indices = new java.util.ArrayList<>();
        if (gpPath == null) return indices;
        for (int i = 0; i < folders.size(); i++) {
            Folder f = folders.get(i);
            String path = f.getPath();
            if (path != null) {
                java.io.File parentFile = new java.io.File(path).getParentFile();
                if (parentFile != null && gpPath.equals(parentFile.getAbsolutePath())) {
                    indices.add(i);
                }
            }
        }
        return indices;
    }

    private void nextPlaylist() {
        if (playlists.isEmpty()) return;
        currentPlaylistIndex = (currentPlaylistIndex + 1) % playlists.size();
        updateUI();
        playCurrentPlaylist();
    }

    private void prevPlaylist() {
        if (playlists.isEmpty()) return;
        currentPlaylistIndex = (currentPlaylistIndex - 1 + playlists.size()) % playlists.size();
        updateUI();
        playCurrentPlaylist();
    }

    // --- Playback Logic ---

    private void playCurrentFolder() {
        if (folders.isEmpty()) return;
        Folder folder = folders.get(currentFolderIndex);
        if (!folder.getTracks().isEmpty()) {
            if (isShuffleEnabled) {
                randomizeCurrentList();
                playTrack(shuffledTracks.get(0));
            } else {
                playTrack(folder.getTracks().get(0));
            }
        }
    }

    private void playCurrentFile() {
        if (folders.isEmpty()) return;
        Track track = folders.get(currentFolderIndex).getTracks().get(currentFileIndex);
        playTrack(track);
    }

    private void playCurrentPlaylist() {
        if (playlists.isEmpty()) return;
        Playlist playlist = playlists.get(currentPlaylistIndex);
        if (!playlist.getTracks().isEmpty()) {
            playTrack(playlist.getTracks().get(0));
        }
    }

    private void playTrack(Track track) {
        playbackManager.play(track.getPath());
        updateNowPlaying(track.getTitle());
        
        // Setup Seek Bar
        progressHandler.removeCallbacks(progressUpdater);
        seekProgress.setMax(playbackManager.getDuration());
        seekProgress.setProgress(0);
        txtTotalTime.setText(formatTime(playbackManager.getDuration()));
        txtCurrentTime.setText("0:00");
        progressHandler.postDelayed(progressUpdater, 1000);
        
        btnPlayPause.setImageResource(R.drawable.ic_pause);
    }

    private void togglePlayPause() {
        if (playbackManager.isPlaying()) {
            playbackManager.pause();
            btnPlayPause.setImageResource(R.drawable.ic_play);
            progressHandler.removeCallbacks(progressUpdater);
        } else {
            playbackManager.resume();
            btnPlayPause.setImageResource(R.drawable.ic_pause);
            progressHandler.postDelayed(progressUpdater, 1000);
        }
    }

    private void toggleShuffle() {
        isShuffleEnabled = !isShuffleEnabled;
        btnShuffle.setSelected(isShuffleEnabled);
        btnShuffle.setAlpha(isShuffleEnabled ? 1.0f : 0.5f);
        if (isShuffleEnabled) {
            randomizeCurrentList();
        }
    }

    private void toggleLoop() {
        isLoopOneEnabled = !isLoopOneEnabled;
        btnLoop.setSelected(isLoopOneEnabled);
        btnLoop.setAlpha(isLoopOneEnabled ? 1.0f : 0.5f);
    }

    private void randomizeCurrentList() {
        if (folders.isEmpty()) return;
        shuffledTracks = new ArrayList<>(folders.get(currentFolderIndex).getTracks());
        Collections.shuffle(shuffledTracks);
    }

    private String formatTime(int msec) {
        int sec = msec / 1000;
        int min = sec / 60;
        sec = sec % 60;
        return String.format("%d:%02d", min, sec);
    }

    private void playNextAuto() {
        if (isLoopOneEnabled) {
            playCurrentFile();
        } else {
            nextFile();
        }
    }

    private void updateUI() {
        updateGrandParentIndexFromFolder();
        if (currentGrandParentIndex >= 0 && currentGrandParentIndex < grandParents.size()) {
            String gpPath = grandParents.get(currentGrandParentIndex);
            String gpName = new java.io.File(gpPath).getName();
            if (gpName == null || gpName.isEmpty()) gpName = gpPath;
            txtGrandParent.setText(gpName);
        } else {
            txtGrandParent.setText("No Grand Parent");
        }

        if (!folders.isEmpty()) {
            Folder f = folders.get(currentFolderIndex);
            txtFolder.setText(f.getName());
            if (!f.getTracks().isEmpty()) {
                txtFile.setText(f.getTracks().get(currentFileIndex).getTitle());
            } else {
                txtFile.setText("No Files");
            }
        }
        if (!playlists.isEmpty()) {
            txtPlaylist.setText(playlists.get(currentPlaylistIndex).getName());
        }
    }

    private void buildGrandParentList() {
        grandParents.clear();
        for (Folder f : folders) {
            String path = f.getPath();
            if (path != null) {
                java.io.File parentFile = new java.io.File(path).getParentFile();
                if (parentFile != null) {
                    String gpPath = parentFile.getAbsolutePath();
                    if (!grandParents.contains(gpPath)) {
                        grandParents.add(gpPath);
                    }
                }
            }
        }
        updateGrandParentIndexFromFolder();
    }

    private void updateGrandParentIndexFromFolder() {
        if (folders.isEmpty() || currentFolderIndex >= folders.size()) {
            currentGrandParentIndex = -1;
            return;
        }
        Folder f = folders.get(currentFolderIndex);
        String path = f.getPath();
        if (path != null) {
            java.io.File parentFile = new java.io.File(path).getParentFile();
            if (parentFile != null) {
                String gpPath = parentFile.getAbsolutePath();
                currentGrandParentIndex = grandParents.indexOf(gpPath);
            }
        }
    }

    private void nextGrandParent() {
        if (grandParents.isEmpty()) return;
        currentGrandParentIndex = (currentGrandParentIndex + 1) % grandParents.size();
        selectFolderFromGrandParent();
    }

    private void prevGrandParent() {
        if (grandParents.isEmpty()) return;
        currentGrandParentIndex = (currentGrandParentIndex - 1 + grandParents.size()) % grandParents.size();
        selectFolderFromGrandParent();
    }

    private void selectFolderFromGrandParent() {
        if (currentGrandParentIndex < 0 || currentGrandParentIndex >= grandParents.size()) return;
        String gpPath = grandParents.get(currentGrandParentIndex);
        for (int i = 0; i < folders.size(); i++) {
            Folder f = folders.get(i);
            String path = f.getPath();
            if (path != null) {
                java.io.File parentFile = new java.io.File(path).getParentFile();
                if (parentFile != null && gpPath.equals(parentFile.getAbsolutePath())) {
                    currentFolderIndex = i;
                    currentFileIndex = 0;
                    updateUI();
                    playCurrentFolder();
                    break;
                }
            }
        }
    }

    private void updateNowPlaying(String title) {
        txtNowPlaying.setText("Now Playing: " + title);
    }

    // --- Permissions ---

    private void checkPermissions() {
        String permission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU 
                ? Manifest.permission.READ_MEDIA_AUDIO 
                : Manifest.permission.READ_EXTERNAL_STORAGE;

        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, PERMISSION_REQUEST_CODE);
        } else {
            loadMedia();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMedia();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadMedia() {
        MediaScanner.scanMedia(this, (folderList, playlistList) -> {
            runOnUiThread(() -> {
                this.folders = folderList;
                this.playlists = playlistList;
                buildGrandParentList();
                
                boolean hasSavedState = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE).contains("saved_active_bar_index");
                if (hasSavedState) {
                    restorePlaybackState();
                } else {
                    if (!folders.isEmpty() || !playlists.isEmpty()) {
                        updateUI();
                    } else {
                        Toast.makeText(this, R.string.no_media_found, Toast.LENGTH_LONG).show();
                    }
                }
            });
        });
    }

    private void setupDummyPlaylists() {
        playlists.add(new Playlist("Favorites"));
        playlists.add(new Playlist("Recent"));
        playlists.add(new Playlist("Road Trip"));
        updateUI();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_help) {
            startActivity(new Intent(this, HelpActivity.class));
        } else if (id == R.id.nav_about) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.nav_folders) {
             startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.nav_settings) {
             startActivity(new Intent(this, GeneralSettingsActivity.class));
        }
        drawerLayout.closeDrawers();
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ? Manifest.permission.READ_MEDIA_AUDIO : Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            loadMedia();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        savePlaybackState();
    }

    private void savePlaybackState() {
        android.content.SharedPreferences prefs = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE);
        boolean rememberState = prefs.getBoolean("remember_playback_state", true);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        if (!rememberState) {
            editor.remove("saved_active_bar_index");
            editor.remove("saved_playlist_index");
            editor.remove("saved_grand_parent_index");
            editor.remove("saved_folder_index");
            editor.remove("saved_file_index");
            editor.remove("saved_playback_position");
            editor.remove("saved_track_path");
            editor.apply();
            return;
        }

        editor.putInt("saved_active_bar_index", activeBarIndex);
        editor.putInt("saved_playlist_index", currentPlaylistIndex);
        editor.putInt("saved_grand_parent_index", currentGrandParentIndex);
        editor.putInt("saved_folder_index", currentFolderIndex);
        editor.putInt("saved_file_index", currentFileIndex);
        
        int position = 0;
        if (playbackManager != null) {
            position = playbackManager.getCurrentPosition();
        }
        editor.putInt("saved_playback_position", position);

        String trackPath = null;
        if (!folders.isEmpty() && currentFolderIndex >= 0 && currentFolderIndex < folders.size()) {
            Folder f = folders.get(currentFolderIndex);
            if (!f.getTracks().isEmpty() && currentFileIndex >= 0 && currentFileIndex < f.getTracks().size()) {
                trackPath = f.getTracks().get(currentFileIndex).getPath();
            }
        }
        editor.putString("saved_track_path", trackPath);
        editor.apply();
    }

    private void restorePlaybackState() {
        android.content.SharedPreferences prefs = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE);
        boolean rememberState = prefs.getBoolean("remember_playback_state", true);
        if (!rememberState) return;

        if (prefs.contains("saved_active_bar_index")) {
            int savedActiveBar = prefs.getInt("saved_active_bar_index", 0);
            int savedPlaylist = prefs.getInt("saved_playlist_index", 0);
            int savedFolder = prefs.getInt("saved_folder_index", 0);
            int savedFile = prefs.getInt("saved_file_index", 0);
            int savedPosition = prefs.getInt("saved_playback_position", 0);
            String savedTrackPath = prefs.getString("saved_track_path", null);

            boolean foundTrack = false;
            if (savedTrackPath != null) {
                for (int fIdx = 0; fIdx < folders.size(); fIdx++) {
                    Folder folder = folders.get(fIdx);
                    for (int tIdx = 0; tIdx < folder.getTracks().size(); tIdx++) {
                        if (savedTrackPath.equals(folder.getTracks().get(tIdx).getPath())) {
                            currentFolderIndex = fIdx;
                            currentFileIndex = tIdx;
                            foundTrack = true;
                            break;
                        }
                    }
                    if (foundTrack) break;
                }
            }

            if (!foundTrack) {
                if (savedFolder >= 0 && savedFolder < folders.size()) {
                    currentFolderIndex = savedFolder;
                    Folder f = folders.get(currentFolderIndex);
                    if (savedFile >= 0 && savedFile < f.getTracks().size()) {
                        currentFileIndex = savedFile;
                    } else {
                        currentFileIndex = 0;
                    }
                }
            }

            if (savedPlaylist >= 0 && savedPlaylist < playlists.size()) {
                currentPlaylistIndex = savedPlaylist;
            }

            updateUI();
            updateActiveBar(savedActiveBar);

            // Now automatically load and play from last position
            if (!folders.isEmpty() && currentFolderIndex >= 0 && currentFolderIndex < folders.size()) {
                Folder currentFolder = folders.get(currentFolderIndex);
                if (!currentFolder.getTracks().isEmpty() && currentFileIndex >= 0 && currentFileIndex < currentFolder.getTracks().size()) {
                    Track track = currentFolder.getTracks().get(currentFileIndex);
                    resumePlayback(track, savedPosition);
                }
            }
        }
    }

    private void resumePlayback(Track track, int position) {
        playbackManager.play(track.getPath());
        if (position > 0 && position < playbackManager.getDuration()) {
            playbackManager.seekTo(position);
        }
        updateNowPlaying(track.getTitle());
        
        // Setup Seek Bar
        progressHandler.removeCallbacks(progressUpdater);
        seekProgress.setMax(playbackManager.getDuration());
        seekProgress.setProgress(position);
        txtTotalTime.setText(formatTime(playbackManager.getDuration()));
        txtCurrentTime.setText(formatTime(position));
        progressHandler.postDelayed(progressUpdater, 1000);
        
        btnPlayPause.setImageResource(R.drawable.ic_pause);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (playbackManager != null) {
            playbackManager.release();
        }
    }
}
