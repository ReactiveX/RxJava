package rx.android.concurrency;

import rx.Scheduler;
import android.os.Handler;

/**
 * Deprecated. Package changed from rx.android.concurrency to rx.android.schedulers.
 * 
 * @deprecated Use {@link rx.android.schedulers.AndroidSchedulers} instead. This will be removed before 1.0 release.
 */
@Deprecated
public class AndroidSchedulers {

    @Deprecated
    public static Scheduler handlerThread(final Handler handler) {
        return rx.android.schedulers.AndroidSchedulers.handlerThread(handler);
    }

    @Deprecated
    public static Scheduler mainThread() {
        return rx.android.schedulers.AndroidSchedulers.mainThread();
    }

}
