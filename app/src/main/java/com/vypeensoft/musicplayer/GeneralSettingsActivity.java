package com.vypeensoft.musicplayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class GeneralSettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_general_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }

        SwitchCompat switchRememberState = findViewById(R.id.switch_remember_state);
        SharedPreferences prefs = getSharedPreferences("MusicPlayerPrefs", Context.MODE_PRIVATE);

        // Load saved state (defaulting to true)
        boolean rememberState = prefs.getBoolean("remember_playback_state", true);
        switchRememberState.setChecked(rememberState);

        switchRememberState.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("remember_playback_state", isChecked).apply();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
