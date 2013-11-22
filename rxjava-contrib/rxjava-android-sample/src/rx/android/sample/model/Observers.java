package rx.android.sample.model;

import rx.Observer;
import rx.android.sample.util.ThreadUtils;
import android.util.Log;

public class Observers {

	/**
	 * Simulates UI callback events - must always be called back on the main thread.
	 */
	public static class LoggingObserver<T> implements Observer<T> {
		private final String logTag;

		public LoggingObserver(String logTag) {
			this.logTag = logTag;
		}

		@Override
		public void onNext(T val) {
			ThreadUtils.assertOnMain();
			Log.v(logTag, "onNext: " + val);
		}

		@Override
		public void onCompleted() {
			ThreadUtils.assertOnMain();
			Log.v(logTag, "onCompleted");
		}

		@Override
		public void onError(Throwable e) {
			ThreadUtils.assertOnMain();
			Log.v(logTag, "onError: " + e);
		}
	}
}
