package com.vypeensoft.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {
    
    private EditText etFolderPaths;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        prefs = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE);
        etFolderPaths = findViewById(R.id.etFolderPaths);
        Button btnSaveSettings = findViewById(R.id.btnSaveSettings);
        Button btnScanFolders = findViewById(R.id.btnScanFolders);

        // Load existing paths
        String savedPaths = prefs.getString("scan_folders", "");
        etFolderPaths.setText(savedPaths);

        btnSaveSettings.setOnClickListener(v -> {
            String paths = etFolderPaths.getText().toString().trim();
            prefs.edit().putString("scan_folders", paths).apply();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnScanFolders.setOnClickListener(v -> {
            String paths = etFolderPaths.getText().toString().trim();
            prefs.edit().putString("scan_folders", paths).apply();
            Toast.makeText(this, "Scanning folders...", Toast.LENGTH_SHORT).show();
            MediaScanner.scanMedia(this, (folderList, playlistList) -> {
                runOnUiThread(() -> {
                    int totalTracks = 0;
                    for (Folder folder : folderList) {
                        totalTracks += folder.getTracks().size();
                    }
                    Toast.makeText(this, "Scan complete! Found " + totalTracks + " tracks in " + folderList.size() + " folders.", Toast.LENGTH_LONG).show();
                });
            });
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
