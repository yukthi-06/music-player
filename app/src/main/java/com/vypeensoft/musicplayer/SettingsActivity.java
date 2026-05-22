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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Folders...");
        }

        etFolderPaths = findViewById(R.id.etFolderPaths);
        Button btnSaveSettings = findViewById(R.id.btnSaveSettings);
        Button btnScanFolders = findViewById(R.id.btnScanFolders);

        // Load existing paths via SettingsManager
        String savedPaths = SettingsManager.loadScanFolders();
        etFolderPaths.setText(savedPaths);

        btnSaveSettings.setOnClickListener(v -> {
            String paths = etFolderPaths.getText().toString().trim();
            SettingsManager.saveScanFolders(paths);
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
            finish();
        });

        btnScanFolders.setOnClickListener(v -> {
            String paths = etFolderPaths.getText().toString().trim();
            SettingsManager.saveScanFolders(paths);
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
