package net.gotev.sipservice;

/**
 * SipService library logger.
 * You can provide your own logger delegate implementation, to be able to log in a different way.
 * By default the log level is set to DEBUG when the build type is debug, and OFF in release.
 * The default logger implementation logs in Android's LogCat.
 * @author gotev (Aleksandar Gotev)
 */
@SuppressWarnings("unused")
public class Logger {

    public enum LogLevel {
        DEBUG,
        INFO,
        ERROR,
        OFF
    }

    public interface LoggerDelegate {
        void error(String tag, String message);
        void error(String tag, String message, Throwable exception);
        void debug(String tag, String message);
        void info(String tag, String message);
    }

    /**
     * Log everything unless otherwise specified by either setLogLevel
     * or by the delegate specified in setLoggerDelegate
     */
    private LogLevel mLogLevel = LogLevel.DEBUG;

    private LoggerDelegate mDelegate = new DefaultLoggerDelegate();

    private Logger() { }

    private static class SingletonHolder {
        private static final Logger instance = new Logger();
    }

    public static void resetLoggerDelegate() {
        synchronized (Logger.class) {
            SingletonHolder.instance.mDelegate = new DefaultLoggerDelegate();
        }
    }

    public static void setLoggerDelegate(LoggerDelegate delegate) {
        if (delegate == null)
            throw new IllegalArgumentException("delegate MUST not be null!");

        synchronized (Logger.class) {
            SingletonHolder.instance.mDelegate = delegate;
        }
    }

    public static void setLogLevel(LogLevel level) {
        synchronized (Logger.class) {
            SingletonHolder.instance.mLogLevel = level;
        }
    }

    public static void error(String tag, String message) {
        if (SingletonHolder.instance.mLogLevel.compareTo(LogLevel.ERROR) <= 0) {
            SingletonHolder.instance.mDelegate.error(tag, message);
        }
    }

    public static void error(String tag, String message, Throwable exception) {
        if (SingletonHolder.instance.mLogLevel.compareTo(LogLevel.ERROR) <= 0) {
            SingletonHolder.instance.mDelegate.error(tag, message, exception);
        }
    }

    public static void info(String tag, String message) {
        if (SingletonHolder.instance.mLogLevel.compareTo(LogLevel.INFO) <= 0) {
            SingletonHolder.instance.mDelegate.info(tag, message);
        }
    }

    public static void debug(String tag, String message) {
        if (SingletonHolder.instance.mLogLevel.compareTo(LogLevel.DEBUG) <= 0) {
            SingletonHolder.instance.mDelegate.debug(tag, message);
        }
    }
}
