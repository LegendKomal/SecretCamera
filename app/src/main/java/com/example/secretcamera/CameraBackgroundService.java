package com.example.secretcamera;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CameraBackgroundService extends Service {
    private static final String TAG = "CameraBackgroundService";
    private static final String CHANNEL_ID = "CameraServiceChannel";
    private static final int NOTIFICATION_ID = 1;

    private CameraDevice cameraDevice;
    private CameraCaptureSession cameraCaptureSession;
    private MediaRecorder mediaRecorder;
    private HandlerThread backgroundThread;
    private Handler backgroundHandler;
    private boolean isRecording = false;
    private String selectedCameraFacing = "BACK";


    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service created");
        createNotificationChannel();
        startBackgroundThread();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service started");
        startForeground(NOTIFICATION_ID, createNotification());
        if (intent.getAction() != null) {
            if (intent.getAction().equals("START_RECORDING")) {
                selectedCameraFacing = intent.getStringExtra("CAMERA_FACING");
                if (selectedCameraFacing == null) {
                    selectedCameraFacing = "BACK";
                }
                startRecording();
            } else if (intent.getAction().equals("STOP_RECORDING")) {
                stopRecording();
            }
        }


        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Camera Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    private Notification createNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Camera Background Service")
                .setContentText("Recording in progress")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
    }

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            Log.e(TAG, "Error stopping background thread: " + e.getMessage());
        }
    }

    private void startRecording() {
        if (isRecording) {
            Log.d(TAG, "Already recording");
            return;
        }
        openCamera();
    }

    private void openCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = selectedCameraFacing.equals("FRONT") ?
                    getFrontCameraId(cameraManager) : getBackCameraId(cameraManager);

            cameraManager.openCamera(cameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                    cameraDevice = camera;
                    Log.d(TAG, "Camera opened");
                    setupMediaRecorder();
                    createCameraCaptureSession();
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                    Log.d(TAG, "Camera disconnected");
                    camera.close();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                    Log.e(TAG, "Camera error: " + error);
                    camera.close();
                }
            }, backgroundHandler);
        } catch (CameraAccessException | SecurityException e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
        }
    }

    private String getFrontCameraId(CameraManager cameraManager) throws CameraAccessException {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                return cameraId;
            }
        }

        return getBackCameraId(cameraManager);
    }

    private String getBackCameraId(CameraManager cameraManager) throws CameraAccessException {
        for (String cameraId : cameraManager.getCameraIdList()) {
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
            Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
            if (facing != null && facing == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }
        throw new CameraAccessException(CameraAccessException.CAMERA_ERROR);
    }

    private void setupMediaRecorder() {
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setOutputFile(getOutputFilePath());
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoSize(1280, 720);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

        try {
            mediaRecorder.prepare();
        } catch (IOException e) {
            Log.e(TAG, "Error preparing MediaRecorder: " + e.getMessage());
        }
    }

    private String getOutputFilePath() {
        File mediaStorageDir = new File(getExternalFilesDir(null), "SecretVideos");

        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.d(TAG, "Failed to create directory");
                return null;
            }
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        return mediaStorageDir.getPath() + File.separator + "VID_" + timeStamp + ".mp4";
    }

    private void createCameraCaptureSession() {
        try {
            List<Surface> surfaces = new ArrayList<>();
            Surface recorderSurface = mediaRecorder.getSurface();
            surfaces.add(recorderSurface);

            cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    cameraCaptureSession = session;
                    Log.d(TAG, "Camera capture session configured");
                    startRecordingVideo();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Failed to configure camera capture session");
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error creating camera capture session: " + e.getMessage());
        }
    }

    private void startRecordingVideo() {
        try {
            CaptureRequest.Builder captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
            captureRequestBuilder.addTarget(mediaRecorder.getSurface());
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_VIDEO);

            cameraCaptureSession.setRepeatingRequest(captureRequestBuilder.build(), null, backgroundHandler);
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "Started recording");
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error starting video recording: " + e.getMessage());
        }
    }

    private void stopRecording() {
        if (!isRecording) {
            Log.d(TAG, "Not recording");
            return;
        }

        try {
            cameraCaptureSession.stopRepeating();
            cameraCaptureSession.abortCaptures();
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error stopping camera capture: " + e.getMessage());
        }

        mediaRecorder.stop();
        mediaRecorder.reset();
        mediaRecorder.release();
        mediaRecorder = null;

        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }

        isRecording = false;
        Log.d(TAG, "Stopped recording");


        Intent intent = new Intent("com.example.secretcamera.RECORDING_STOPPED");
        sendBroadcast(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopRecording();
        stopBackgroundThread();
        Log.d(TAG, "Service destroyed");
    }
}