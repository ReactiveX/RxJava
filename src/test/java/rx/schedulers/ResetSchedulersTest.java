package rx.schedulers;


import org.junit.Test;
import rx.Scheduler;
import rx.internal.schedulers.*;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;

import static org.junit.Assert.assertTrue;

public class ResetSchedulersTest {

    @Test
    public void reset() {
        RxJavaPlugins.getInstance().reset();

        final TestScheduler testScheduler = new TestScheduler();
        RxJavaPlugins.getInstance().registerSchedulersHook(new RxJavaSchedulersHook() {
            @Override
            public Scheduler getComputationScheduler() {
                return testScheduler;
            }

            @Override
            public Scheduler getIOScheduler() {
                return testScheduler;
            }

            @Override
            public Scheduler getNewThreadScheduler() {
                return testScheduler;
            }
        });
        Schedulers.reset();

        assertTrue(Schedulers.io().equals(testScheduler));
        assertTrue(Schedulers.computation().equals(testScheduler));
        assertTrue(Schedulers.newThread().equals(testScheduler));

        RxJavaPlugins.getInstance().reset();
        RxJavaPlugins.getInstance().registerSchedulersHook(RxJavaSchedulersHook.getDefaultInstance());
        Schedulers.reset();

        assertTrue(Schedulers.io() instanceof CachedThreadScheduler);
        assertTrue(Schedulers.computation() instanceof EventLoopsScheduler);
        assertTrue(Schedulers.newThread() instanceof rx.internal.schedulers.NewThreadScheduler);
    }

}
