package rx.android.sample.util;

public class ThreadUtils {

	private ThreadUtils() {}

	public static void assertOnMain() {
		if ( ! "main".equals(Thread.currentThread().getName())) {
			throw new IllegalStateException("Not on main thread when we should be.");
		}
	}

	public static void assertNotOnMain() {
		if ("main".equals(Thread.currentThread().getName())) {
			throw new IllegalStateException("On main thread when we should not be.");
		}
	}
}
