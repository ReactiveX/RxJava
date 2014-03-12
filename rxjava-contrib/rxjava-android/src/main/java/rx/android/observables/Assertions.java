package rx.android.observables;

import android.os.Looper;

public class Assertions {
    public static void assertUiThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalStateException("Observers must subscribe from the main UI thread, but was " + Thread.currentThread());
        }
    }
}
