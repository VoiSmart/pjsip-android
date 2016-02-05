package net.gotev.sipservice;

import android.util.Log;

/**
 * Default logger delegate implementation which logs in LogCat with {@link Log}.
 * @author gotev (Aleksandar Gotev)
 */
public class DefaultLoggerDelegate implements Logger.LoggerDelegate {
    @Override
    public void error(String tag, String message) {
        Log.e(tag, message);
    }

    @Override
    public void error(String tag, String message, Throwable exception) {
        Log.e(tag, message, exception);
    }

    @Override
    public void debug(String tag, String message) {
        Log.d(tag, message);
    }

    @Override
    public void info(String tag, String message) {
        Log.i(tag, message);
    }
}
