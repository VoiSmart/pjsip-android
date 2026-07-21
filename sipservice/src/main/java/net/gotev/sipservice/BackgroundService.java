package net.gotev.sipservice;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Service with a background worker thread.
 */
class BackgroundService extends Service {

    // Name of the single thread that owns the whole pjsua2 stack. Both SIP command
    // handlers (posted via enqueueJob) and — once threadCnt=0 + the libHandleEvents
    // pump are in place — all PJSIP callbacks run here, so nothing else ever touches
    // a native SipCall concurrently. This is what closes the use-after-free race
    // (Crashlytics b4d5d850… onCallState SIGSEGV): previously commands ran on the
    // main looper while PJSIP fired callbacks on its own worker thread.
    static final String SIP_THREAD_NAME = "SipThread";

    private HandlerThread mSipThread;
    private Handler mHandler;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        // Dedicated background thread instead of the main looper: keeps SIP work
        // off the UI thread (no ANRs) and, together with threadCnt=0, confines all
        // pjsua2 access to a single thread.
        mSipThread = new HandlerThread(SIP_THREAD_NAME);
        mSipThread.start();
        mHandler = new Handler(mSipThread.getLooper());
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
        if (mSipThread != null) {
            // quitSafely() drains jobs already enqueued before this point — notably
            // the stopStack() posted by SipService.onDestroy() — before the looper
            // terminates, so the stack is torn down on SipThread as intended.
            mSipThread.quitSafely();
            mSipThread = null;
        }
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
