package net.gotev.sipservice;

import android.app.Service;
import android.content.Intent;
import android.os.*;
import android.os.Process;

/**
 * Service with a background worker thread.
 * @author gotev (Aleksandar Gotev)
 */
class BackgroundService extends Service {

    private HandlerThread mWorkerThread;
    private Handler mHandler;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());

        mWakeLock.acquire();

        mWorkerThread = new HandlerThread(getClass().getSimpleName(), Process.THREAD_PRIORITY_FOREGROUND);
        mWorkerThread.setPriority(Thread.MAX_PRIORITY);
        mWorkerThread.start();
        mHandler = new Handler(mWorkerThread.getLooper());
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mWorkerThread.quitSafely();
        mWakeLock.release();
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
}
