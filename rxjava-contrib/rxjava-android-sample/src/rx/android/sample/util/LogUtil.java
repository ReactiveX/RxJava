package rx.android.sample.util;

import android.util.Log;

public class LogUtil {

	public static void handleException(String tag, Throwable th) {
		Log.e(tag, th.getMessage(), th);
	}
}
