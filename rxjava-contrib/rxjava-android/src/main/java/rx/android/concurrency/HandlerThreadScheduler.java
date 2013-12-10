package rx.android.concurrency;

import android.os.Handler;

/**
 * Deprecated. Package changed from rx.android.concurrency to rx.android.schedulers.
 * 
 * @deprecated Use {@link rx.android.schedulers.HandlerThreadScheduler} instead. This will be removed before 1.0 release.
 */
@Deprecated
public class HandlerThreadScheduler extends rx.android.schedulers.HandlerThreadScheduler {

    public HandlerThreadScheduler(Handler handler) {
        super(handler);
    }

}
