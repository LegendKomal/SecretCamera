package com.example.secretcamera;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;

public class PhoneCallReceiver extends BroadcastReceiver {

    private static final String TAG = "DialerReceiver";
    private static final String SECRET_CODE = "*#*#1234#*#*";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (TelephonyManager.ACTION_PHONE_STATE_CHANGED.equals(intent.getAction())) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            if (TelephonyManager.EXTRA_STATE_IDLE.equals(state)) {
                // Check if the dialed number matches the secret code
                String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                if (incomingNumber != null && incomingNumber.equals(SECRET_CODE)) {
                    // Launch the MainActivity with the UNHIDDEN flag
                    Intent activityIntent = new Intent(context, MainActivity.class);
                    activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activityIntent.putExtra("UNHIDDEN", true);
                    context.startActivity(activityIntent);
                }
            }
        }
    }
}

