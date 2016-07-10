package rx.internal.schedulers;

import java.util.concurrent.*;

import rx.functions.Func0;
import rx.internal.util.RxThreadFactory;
import rx.plugins.RxJavaHooks;

/**
 * Utility class to create the individual ScheduledExecutorService instances for
 * the GenericScheduledExecutorService class.
 */
enum GenericScheduledExecutorServiceFactory {
    ;
    
    static final String THREAD_NAME_PREFIX = "RxScheduledExecutorPool-";
    static final RxThreadFactory THREAD_FACTORY = new RxThreadFactory(THREAD_NAME_PREFIX);

    static ThreadFactory threadFactory() {
        return THREAD_FACTORY;
    }
    
    /**
     * Creates a ScheduledExecutorService (either the default or given by a hook).
     * @return the SchuduledExecutorService created.
     */
    public static ScheduledExecutorService create() {
        Func0<? extends ScheduledExecutorService> f = RxJavaHooks.getOnGenericScheduledExecutorService();
        if (f == null) {
            return createDefault();
        }
        return f.call();
    }
    
    
    static ScheduledExecutorService createDefault() {
        return Executors.newScheduledThreadPool(1, threadFactory());
    }
}
