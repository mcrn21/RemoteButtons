package com.mcrn21.remotebuttons;

import android.app.ActivityOptions;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

public class Commands {
    public static void left(RemoteButtonsService service) {
        AudioManager audio = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
    }

    public static void right(RemoteButtonsService service) {
        AudioManager audio = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        audio.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI);
    }

    public static void enter(RemoteButtonsService service) {
        AudioManager audio = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        if (audio.isMusicActive()) {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE);
            audio.dispatchMediaKeyEvent(event);
        } else {
            KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY);
            audio.dispatchMediaKeyEvent(event);
        }
    }

    public static void left_up(RemoteButtonsService service) {
        AudioManager audio = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS);
        audio.dispatchMediaKeyEvent(event);
    }

    public static void left_down(RemoteButtonsService service) {
        AudioManager audio = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        if (audio.isStreamMute(AudioManager.STREAM_MUSIC))
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0);
        else
            audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, 0);
    }

    public static void right_up(RemoteButtonsService service) {
        AudioManager audio = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        KeyEvent event = new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT);
        audio.dispatchMediaKeyEvent(event);
    }

    public static void right_down(RemoteButtonsService service) {
        String packageName = Settings.getInstance().launchAppPackageName;
        if (!Settings.getInstance().launchAppEnable || packageName.isEmpty())
            return;

        Intent intent = service.getPackageManager().getLaunchIntentForPackage(packageName);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                service, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        try {
            pendingIntent.send();
        } catch (PendingIntent.CanceledException e) {
            throw new RuntimeException(e);
        }
    }
}
