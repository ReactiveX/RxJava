package rx.android.concurrency;

import android.os.Handler;
import android.os.Looper;
import rx.Scheduler;

/**
 * Schedulers that have Android specific functionality
 */
public class AndroidSchedulers {

    private static final Scheduler MAIN_THREAD_SCHEDULER =
            new HandlerThreadScheduler(new Handler(Looper.getMainLooper()));

    private AndroidSchedulers(){

    }

    /**
     * {@link Scheduler} which uses the provided {@link Handler} to execute an action
     * @param handler The handler that will be used when executing the action
     * @return A handler based scheduler
     */
    public static Scheduler handlerThread(final Handler handler) {
        return new HandlerThreadScheduler(handler);
    }

    /**
     * {@link Scheduler} which will execute an action on the main Android UI thread.
     *
     * @return A Main {@link Looper} based scheduler
     */
    public static Scheduler mainThread() {
        return MAIN_THREAD_SCHEDULER;
    }
}
