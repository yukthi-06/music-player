package com.vypeensoft.musicplayer;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("About");
        }

        TextView tvAppVersion = findViewById(R.id.tvAppVersion);
        if (tvAppVersion != null) {
            String versionInfo = String.format("Version: %s\nBuild: %s\nGit SHA: %s\nGit SHA Full: %s\nGit Tag: %s",
                    BuildConfig.VERSION_NAME,
                    BuildConfig.BUILD_TIMESTAMP,
                    BuildConfig.GIT_SHA,
                    BuildConfig.GIT_SHA_FULL,
                    BuildConfig.GIT_TAG);
            tvAppVersion.setText(versionInfo);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
