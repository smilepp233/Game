package com.example.groupproject_game;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service {
    private static final String TAG = "MusicService";
    private MediaPlayer mediaPlayer;
    private final IBinder binder = new MusicBinder();
    private boolean isPlaying = false;

    public class MusicBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "音乐服务已创建");
        mediaPlayer = MediaPlayer.create(this, R.raw.main_song);
        mediaPlayer.setLooping(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "PLAY":
                        play();
                        break;
                    case "PAUSE":
                        pause();
                        break;
                    case "TOGGLE":
                        toggle();
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void play() {
        if (!mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            isPlaying = true;
            Log.d(TAG, "音乐开始播放");
        }
    }

    public void pause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            isPlaying = false;
            Log.d(TAG, "音乐暂停");
        }
    }

    public void toggle() {
        if (mediaPlayer.isPlaying()) {
            pause();
        } else {
            play();
        }
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    @Override
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
        Log.d(TAG, "音乐服务已销毁");
    }
} 