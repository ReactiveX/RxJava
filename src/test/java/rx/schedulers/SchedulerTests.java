package rx.schedulers;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

final class SchedulerTests {
    private SchedulerTests() {
        // No instances.
    }

    /**
     * Verifies that the given Scheduler delivers unhandled errors to its executing thread's
     * {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * Schedulers which execute on a separate thread from their calling thread should exhibit this behavior. Schedulers
     * which execute on their calling thread may not.
     */
    static void testUnhandledErrorIsDeliveredToThreadHandler(Scheduler scheduler) throws InterruptedException {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        try {
            CapturingUncaughtExceptionHandler handler = new CapturingUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(handler);
            IllegalStateException error = new IllegalStateException("Should be delivered to handler");
            Observable.error(error)
                    .subscribeOn(scheduler)
                    .subscribe();

            if (!handler.completed.await(3, TimeUnit.SECONDS)) {
                fail("timed out");
            }

            assertEquals("Should have received exactly 1 exception", 1, handler.count);
            Throwable cause = handler.caught;
            while (cause != null) {
                if (error.equals(cause)) break;
                if (cause == cause.getCause()) break;
                cause = cause.getCause();
            }
            assertEquals("Our error should have been delivered to the handler", error, cause);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }

    /**
     * Verifies that the given Scheduler does not deliver handled errors to its executing Thread's
     * {@link java.lang.Thread.UncaughtExceptionHandler}.
     * <p>
     * This is a companion test to {@link #testUnhandledErrorIsDeliveredToThreadHandler}, and is needed only for the
     * same Schedulers.
     */
    static void testHandledErrorIsNotDeliveredToThreadHandler(Scheduler scheduler) throws InterruptedException {
        Thread.UncaughtExceptionHandler originalHandler = Thread.getDefaultUncaughtExceptionHandler();
        try {
            CapturingUncaughtExceptionHandler handler = new CapturingUncaughtExceptionHandler();
            CapturingObserver<Object> observer = new CapturingObserver<Object>();
            Thread.setDefaultUncaughtExceptionHandler(handler);
            IllegalStateException error = new IllegalStateException("Should be delivered to handler");
            Observable.error(error)
                    .subscribeOn(scheduler)
                    .subscribe(observer);

            if (!observer.completed.await(3, TimeUnit.SECONDS)) {
                fail("timed out");
            }

            assertEquals("Handler should not have received anything", 0, handler.count);
            assertEquals("Observer should have received an error", 1, observer.errorCount);
            assertEquals("Observer should not have received a next value", 0, observer.nextCount);

            Throwable cause = observer.error;
            while (cause != null) {
                if (error.equals(cause)) break;
                if (cause == cause.getCause()) break;
                cause = cause.getCause();
            }
            assertEquals("Our error should have been delivered to the observer", error, cause);
        } finally {
            Thread.setDefaultUncaughtExceptionHandler(originalHandler);
        }
    }

    private static final class CapturingUncaughtExceptionHandler implements Thread.UncaughtExceptionHandler {
        int count = 0;
        Throwable caught;
        CountDownLatch completed = new CountDownLatch(1);

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            count++;
            caught = e;
            completed.countDown();
        }
    }

    private static final class CapturingObserver<T> implements Observer<T> {
        CountDownLatch completed = new CountDownLatch(1);
        int errorCount = 0;
        int nextCount = 0;
        Throwable error;

        @Override
        public void onCompleted() {
        }

        @Override
        public void onError(Throwable e) {
            errorCount++;
            error = e;
            completed.countDown();
        }

        @Override
        public void onNext(T t) {
            nextCount++;
        }
    }
}
