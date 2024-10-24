package com.example.secretcamera;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import java.io.File;

public class VideoPlayerActivity extends AppCompatActivity {
    private static final String TAG = "VideoPlayerActivity";
    private VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        videoView = findViewById(R.id.videoView);
        String videoPath = getIntent().getStringExtra("videoPath");

        if (videoPath != null) {
            try {
                File videoFile = new File(videoPath);
                Uri videoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".fileprovider", videoFile);
                videoView.setVideoURI(videoUri);

                MediaController mediaController = new MediaController(this);
                mediaController.setAnchorView(videoView);
                videoView.setMediaController(mediaController);

                videoView.setOnErrorListener((mp, what, extra) -> {
                    Log.e(TAG, "Error playing video: what=" + what + " extra=" + extra);
                    Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
                    finish();
                    return true;
                });

                videoView.setOnPreparedListener(mp -> {
                    mp.setOnVideoSizeChangedListener((mediaPlayer, width, height) -> {
                        MediaController mediaController1 = new MediaController(VideoPlayerActivity.this);
                        videoView.setMediaController(mediaController1);
                        mediaController1.setAnchorView(videoView);
                    });
                });

                videoView.start();
            } catch (Exception e) {
                Log.e(TAG, "Error setting up video: " + e.getMessage());
                Toast.makeText(this, "Error playing video", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            Toast.makeText(this, "Error: Video not found", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoView != null) {
            videoView.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoView != null) {
            videoView.stopPlayback();
        }
    }
}