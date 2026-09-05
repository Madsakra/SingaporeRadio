package com.oai.singaporeradio;

import android.app.*;
import android.content.*;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

public class RadioService extends Service {
    public static final String ACTION_PLAY = "com.oai.singaporeradio.PLAY";
    public static final String ACTION_STOP = "com.oai.singaporeradio.STOP";
    public static final String EXTRA_URL = "url";
    public static final String EXTRA_NAME = "name";
    public static final String BROADCAST_STATUS = "com.oai.singaporeradio.STATUS";
    private static final String CHANNEL_ID = "radio_playback";
    private static volatile String latestStatus = "Stopped";
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExoPlayer player;
    private String currentName = "Singapore Radio";
    private final Runnable connectionTimeout = () -> fail(
        "Connection timed out. Check Wi-Fi or mobile data, then tap PLAY to retry.");

    public static String getLatestStatus() { return latestStatus; }

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Radio playback", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Shows controls while radio is playing");
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) { stopSelf(); return START_NOT_STICKY; }
        if (ACTION_STOP.equals(intent.getAction())) {
            stopPlayback();
        } else if (ACTION_PLAY.equals(intent.getAction())) {
            String url = intent.getStringExtra(EXTRA_URL);
            currentName = intent.getStringExtra(EXTRA_NAME);
            // Satisfy the foreground-service deadline even when the network is unavailable.
            startForeground(7, notification("Connecting…"));
            if (url == null || !url.startsWith("https://")) {
                fail("This station is unavailable. Please choose another station.");
            } else {
                play(url);
            }
        }
        return START_NOT_STICKY;
    }

    @androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
    private void play(String url) {
        releasePlayer();
        ConnectivityManager cm = getSystemService(ConnectivityManager.class);
        NetworkCapabilities network = cm.getNetworkCapabilities(cm.getActiveNetwork());
        if (network == null || !network.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) {
            fail("No internet connection. Turn on Wi-Fi or mobile data, then tap PLAY.");
            return;
        }
        sendStatus("Connecting to " + currentName + "…");
        DefaultHttpDataSource.Factory http = new DefaultHttpDataSource.Factory()
            .setUserAgent("SingaporeRadio/1.1")
            .setConnectTimeoutMs(10000)
            .setReadTimeoutMs(10000);
        final ExoPlayer active = new ExoPlayer.Builder(this)
            .setMediaSourceFactory(new DefaultMediaSourceFactory(http))
            .setLoadControl(new DefaultLoadControl.Builder()
                .setBufferDurationsMs(3000, 15000, 1000, 2000)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build())
            .build();
        player = active;
        active.setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(), true);
        active.setHandleAudioBecomingNoisy(true);
        active.setWakeMode(C.WAKE_MODE_LOCAL);
        active.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if (player != active) return;
                if (state == Player.STATE_BUFFERING) {
                    handler.removeCallbacks(connectionTimeout);
                    handler.postDelayed(connectionTimeout, 20000);
                    if (latestStatus.startsWith("Playing")) sendStatus("Buffering " + currentName + "…");
                } else if (state == Player.STATE_READY) {
                    handler.removeCallbacks(connectionTimeout);
                    if (!active.isPlaying()) sendStatus("Paused " + currentName + ". Tap PLAY to resume.");
                } else if (state == Player.STATE_ENDED) {
                    fail("The station stopped streaming. Tap PLAY to reconnect.");
                }
            }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                if (player != active) return;
                if (isPlaying) {
                    handler.removeCallbacks(connectionTimeout);
                    sendStatus("Playing " + currentName);
                    getSystemService(NotificationManager.class).notify(7, notification("Playing"));
                } else if (active.getPlaybackState() == Player.STATE_READY) {
                    sendStatus("Paused " + currentName + ". Tap PLAY to resume.");
                    getSystemService(NotificationManager.class).notify(7, notification("Paused"));
                }
            }
            @Override public void onPlayerError(PlaybackException error) {
                if (player != active) return;
                Log.e("SingaporeRadio", "Playback failed: " + error.getErrorCodeName(), error);
                fail("Unable to play " + currentName + ". Check your connection or try another station.");
            }
        });
        handler.postDelayed(connectionTimeout, 20000);
        active.setMediaItem(MediaItem.fromUri(url));
        active.prepare();
        active.play();
    }

    private Notification notification(String state) {
        PendingIntent content = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, RadioService.class).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CHANNEL_ID) : new Notification.Builder(this);
        return b.setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(currentName == null ? "Singapore Radio" : currentName)
            .setContentText(state).setContentIntent(content).setOngoing(true)
            .addAction(new Notification.Action.Builder(android.R.drawable.ic_media_pause, "Stop", stop).build())
            .build();
    }

    private void sendStatus(String status) {
        latestStatus = status;
        Log.i("SingaporeRadio", status);
        sendBroadcast(new Intent(BROADCAST_STATUS).setPackage(getPackageName()).putExtra("status", status));
    }

    private void releasePlayer() {
        handler.removeCallbacks(connectionTimeout);
        ExoPlayer old = player;
        player = null;
        if (old != null) old.release();
    }

    private void fail(String message) {
        releasePlayer();
        sendStatus(message);
        stopForeground(true);
        stopSelf();
    }

    private void stopPlayback() {
        releasePlayer();
        sendStatus("Stopped");
        stopForeground(true);
        stopSelf();
    }

    @Override public void onDestroy() {
        boolean wasActive = player != null;
        releasePlayer();
        if (wasActive) sendStatus("Stopped");
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
