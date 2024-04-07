package com.mcrn21.remotebuttons;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartServiceOnBootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, RemoteButtonsService.class);
            context.startForegroundService(serviceIntent);
        }
    }
}
