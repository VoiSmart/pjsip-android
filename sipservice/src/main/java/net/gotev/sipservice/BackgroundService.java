package net.gotev.sipservice;

import android.app.Service;
import android.content.Intent;
import android.os.*;

/**
 * Service with a background worker thread.
 * @author gotev (Aleksandar Gotev)
 */
class BackgroundService extends Service {

    private Handler mHandler;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler(Looper.getMainLooper());
        acquireWakeLock();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseWakeLock();
    }

    protected void enqueueJob(Runnable job) {
        mHandler.post(job);
    }

    protected void enqueueDelayedJob(Runnable job, long delayMillis) {
        mHandler.postDelayed(job, delayMillis);
    }

    protected void dequeueJob(Runnable job) {
        mHandler.removeCallbacks(job);
    }

    public void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
        mWakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
        }
    }
}
