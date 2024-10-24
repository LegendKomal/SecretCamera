package com.example.secretcamera;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity implements VideoAdapter.OnVideoClickListener, VideoAdapter.OnVideoDeleteListener {
    private static final int PERMISSION_REQUEST_CODE = 100;
    private Button startRecordingButton;
    private Button stopRecordingButton;
    private RecyclerView videoRecyclerView;
    private VideoAdapter videoAdapter;
    private List<File> videoFiles;
    private boolean isRecording = false;
    private TextView noVideosText;
    private static final String CAMERA_FACING_FRONT = "FRONT";
    private static final String CAMERA_FACING_BACK = "BACK";
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean allGranted = true;
                        for (Boolean granted : result.values()) {
                            if (!granted) {
                                allGranted = false;
                                break;
                            }
                        }
                        if (allGranted) {
                            startRecording();
                        } else {
                            Toast.makeText(this, "Required permissions not granted", Toast.LENGTH_SHORT).show();
                        }
                    });

    private final ActivityResultLauncher<Intent> storagePermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            if (Environment.isExternalStorageManager()) {
                                startRecording();
                            } else {
                                Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    private final BroadcastReceiver recordingStoppedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("com.example.secretcamera.RECORDING_STOPPED")) {
                isRecording = false;
                loadVideos();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        setupRecyclerView();
        setupButtonListeners();
        loadVideos();

        IntentFilter filter = new IntentFilter("com.example.secretcamera.RECORDING_STOPPED");
        registerReceiver(recordingStoppedReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        if (getIntent().getBooleanExtra("UNHIDDEN", false)) {
            // Handle app being unhidden (e.g., show a welcome message)
            Toast.makeText(this, "App unhidden!", Toast.LENGTH_SHORT).show();
            // You can also perform any other actions necessary when the app is unhidden
        }
    }

    private void showCameraSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Camera")
                .setItems(new String[]{"Front Camera", "Back Camera"}, (dialog, which) -> {
                    String cameraFacing = (which == 0) ? CAMERA_FACING_FRONT : CAMERA_FACING_BACK;
                    startRecordingWithCamera(cameraFacing);
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void startRecordingWithCamera(String cameraFacing) {
        Intent serviceIntent = new Intent(this, CameraBackgroundService.class);
        serviceIntent.setAction("START_RECORDING");
        serviceIntent.putExtra("CAMERA_FACING", cameraFacing);
        startForegroundService(serviceIntent);
        isRecording = true;
        Toast.makeText(this, "Recording started with " +
                        (cameraFacing.equals(CAMERA_FACING_FRONT) ? "front" : "back") + " camera",
                Toast.LENGTH_SHORT).show();
    }

    private void initializeViews() {
        startRecordingButton = findViewById(R.id.startServiceButton);
        stopRecordingButton = findViewById(R.id.stopRecordingButton);
        videoRecyclerView = findViewById(R.id.videoRecyclerView);
        noVideosText = findViewById(R.id.noVideosText);
    }

    private void setupRecyclerView() {
        videoRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        videoFiles = new ArrayList<>();
        videoAdapter = new VideoAdapter(this, videoFiles, this, this);
        videoRecyclerView.setAdapter(videoAdapter);
    }

    private void setupButtonListeners() {
        startRecordingButton.setOnClickListener(v -> {
            if (isRecording) {
                Toast.makeText(this, "Recording is already in progress", Toast.LENGTH_SHORT).show();
            } else if (checkPermissions()) {
                startRecording();
            } else {
                requestPermissions();
            }
        });

        stopRecordingButton.setOnClickListener(v -> {
            if (!isRecording) {
                Toast.makeText(this, "Start recording first", Toast.LENGTH_SHORT).show();
            } else {
                stopRecording();
                loadVideos();
            }
        });
    }

    private void loadVideos() {
        File directory = new File(getExternalFilesDir(null), "SecretVideos");
        if (directory.exists()) {
            File[] files = directory.listFiles((dir, name) -> name.endsWith(".mp4"));
            if (files != null) {
                videoFiles.clear();
                videoFiles.addAll(Arrays.asList(files));
                videoAdapter.updateVideos(videoFiles);
                updateNoVideosVisibility();
            }
        }
    }

    private void updateNoVideosVisibility() {
        if (videoFiles.isEmpty()) {
            noVideosText.setVisibility(View.VISIBLE);
            videoRecyclerView.setVisibility(View.GONE);
        } else {
            noVideosText.setVisibility(View.GONE);
            videoRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onVideoClick(File videoFile) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra("videoPath", videoFile.getAbsolutePath());
        startActivity(intent);
    }

    @Override
    public void onVideoDelete(File videoFile, int position) {
        if (videoFile.delete()) {
            videoFiles.remove(position);
            videoAdapter.notifyItemRemoved(position);
            videoAdapter.notifyItemRangeChanged(position, videoFiles.size());
            updateNoVideosVisibility();
            Toast.makeText(this, "Video deleted successfully", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Failed to delete video", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkPermissions() {
        boolean mandatoryPermissions = checkBasicPermissions();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return mandatoryPermissions && Environment.isExternalStorageManager();
        } else {
            return mandatoryPermissions &&
                    ContextCompat.checkSelfPermission(this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                            PackageManager.PERMISSION_GRANTED;
        }
    }

    private boolean checkBasicPermissions() {
        boolean cameraPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
        boolean audioPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        boolean storagePermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;

        boolean notificationPermission = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission = ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        return cameraPermission && audioPermission && storagePermission && notificationPermission;
    }


    private void requestPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);
        permissions.add(Manifest.permission.RECORD_AUDIO);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }

        permissionLauncher.launch(permissions.toArray(new String[0]));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(android.net.Uri.parse(
                        String.format("package:%s", getApplicationContext().getPackageName())
                ));
                storagePermissionLauncher.launch(intent);
            } catch (Exception e) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                storagePermissionLauncher.launch(intent);
            }
        }
    }

    private void startRecording() {
        showCameraSelectionDialog();
    }

    private void stopRecording() {
        Intent serviceIntent = new Intent(this, CameraBackgroundService.class);
        serviceIntent.setAction("STOP_RECORDING");
        startForegroundService(serviceIntent);
        Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(recordingStoppedReceiver);
    }
}