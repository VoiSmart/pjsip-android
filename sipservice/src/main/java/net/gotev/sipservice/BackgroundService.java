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

    private static final String TAG = BackgroundService.class.getSimpleName();

    // Name of the single thread that owns the whole pjsua2 stack. Both SIP command
    // handlers (posted via enqueueJob) and — thanks to threadCnt=0 plus the
    // libHandleEvents pump — all PJSIP callbacks run here, so nothing else ever
    // touches a native SipCall concurrently. Commands used to run on the main looper
    // while PJSIP fired callbacks on its own worker thread, which raced.
    static final String SIP_THREAD_NAME = "SipThread";

    /*
     * The thread is PROCESS-GLOBAL, not per-service-instance, and is never quit.
     *
     * onDestroy() only *posts* stopStack(), so it returns long before libDestroy()
     * finishes (observed at 0.1–1.4s depending on the device). Android then happily
     * creates a brand new SipService for the next startService(). When each instance
     * owned its own HandlerThread, that new instance's thread ran pjsua2 calls
     * CONCURRENTLY with the previous instance's teardown — on a thread pjlib had
     * never been told about — which aborts in pj_thread_this():
     *
     *   "Calling pjlib from unknown/external thread. You must register external
     *    threads with pj_thread_register() before calling any pjlib functions."
     *
     * Before SIP work moved off the main looper, both instances shared that one
     * looper — a single FIFO queue — which serialized them for free; making the
     * thread per-instance silently dropped that guarantee.
     *
     * One global serial queue restores the invariant: instance B's onCreate job is
     * always enqueued BEHIND instance A's stopStack job, and there is only ever one
     * thread for pjlib to know about.
     *
     * Cost of never quitting it: an idle HandlerThread blocks in Looper.loop() ->
     * MessageQueue.next() -> epoll_wait, so it is never scheduled — no CPU, no
     * battery, no wakelock, no effect on doze or the app freezer, and it does not
     * keep the process alive. It costs one OS thread (task struct + a mostly-unused
     * stack). Against that we stop creating and destroying a thread on every
     * service start/stop cycle, so this is a marginal win rather than a cost.
     */
    private static HandlerThread sSipThread;
    private static Handler sHandler;

    private PowerManager.WakeLock mWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        initSipThread();
        acquireWakeLock();
    }

    private static synchronized void initSipThread() {
        if (sSipThread != null) return;
        sSipThread = new HandlerThread(SIP_THREAD_NAME);
        sSipThread.start();
        sHandler = new Handler(sSipThread.getLooper());
        Logger.debug(TAG, "Started process-global " + SIP_THREAD_NAME);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // The wake lock is deliberately NOT released here: onDestroy returns before
        // the stopStack() job it posted has run, so releasing now would let the CPU
        // suspend part-way through libDestroy() — stalling teardown and widening the
        // very window this class exists to close. SipService releases it at the end
        // of its own onDestroy job instead, once teardown has finished.
        //
        // The SipThread is likewise NOT quit: it is process-global (see above), and
        // quitting it here would race the stopStack job still queued on it.
    }

    protected void enqueueJob(Runnable job) {
        sHandler.post(job);
    }

    protected void enqueueDelayedJob(Runnable job, long delayMillis) {
        sHandler.postDelayed(job, delayMillis);
    }

    protected void dequeueJob(Runnable job) {
        sHandler.removeCallbacks(job);
    }

    public void acquireWakeLock() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, getClass().getSimpleName());
        mWakeLock.acquire();
    }

    public void releaseWakeLock() {
        if (mWakeLock != null && mWakeLock.isHeld()) {
            mWakeLock.release();
            Logger.debug(TAG, "Wake lock released");
        }
    }
}
