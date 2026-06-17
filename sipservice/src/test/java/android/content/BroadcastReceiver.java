package android.content;

/**
 * Minimal test-only stub shadowing the framework {@link android.content.BroadcastReceiver}
 * so classes that extend it (e.g. {@code BroadcastEventReceiver}) can be instantiated in
 * plain JVM unit tests without Robolectric. Mirrors the existing {@code android.util.Log}
 * shadow already present in this test source set.
 */
public class BroadcastReceiver {
    public BroadcastReceiver() {
    }
}
