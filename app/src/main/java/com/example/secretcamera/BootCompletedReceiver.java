package com.example.secretcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

public class BootCompletedReceiver extends BroadcastReceiver {
    private static final String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction()) ||
                "com.example.secretcamera.SERVICE_RESTART".equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, CameraBackgroundService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}