package rx.operators;

import static org.junit.Assert.*;

import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

/**
 * Common utility functions for operator implementations and tests.
 */
/* package */class AbstractOperation
{
    private AbstractOperation() {
    }

    public static class UnitTest {

        public static <T> Func1<Observer<T>, Subscription> assertTrustedObservable(final Func1<Observer<T>, Subscription> source)
        {
            return new Func1<Observer<T>, Subscription>()
            {
                @Override
                public Subscription call(Observer<T> observer)
                {
                    return source.call(new TestingObserver<T>(observer));
                }
            };
        }

        public static class TestingObserver<T> implements Observer<T> {

            private final Observer<T> actual;
            private final AtomicBoolean isFinished = new AtomicBoolean(false);
            private final AtomicBoolean isInCallback = new AtomicBoolean(false);

            public TestingObserver(Observer<T> actual) {
                this.actual = actual;
            }

            @Override
            public void onCompleted() {
                assertFalse("previous call to onCompleted() or onError()", !isFinished.compareAndSet(false, true));
                assertFalse("concurrent callback pending", !isInCallback.compareAndSet(false, true));
                actual.onCompleted();
                isInCallback.set(false);
            }

            @Override
            public void onError(Exception e) {
                assertFalse("previous call to onCompleted() or onError()", !isFinished.compareAndSet(false, true));
                assertFalse("concurrent callback pending", !isInCallback.compareAndSet(false, true));
                actual.onError(e);
                isInCallback.set(false);
            }

            @Override
            public void onNext(T args) {
                assertFalse("previous call to onCompleted() or onError()", isFinished.get());
                assertFalse("concurrent callback pending", !isInCallback.compareAndSet(false, true));
                actual.onNext(args);
                isInCallback.set(false);
            }

        }

        @Test(expected = AssertionError.class)
        public void testDoubleCompleted() {
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onCompleted();
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            })).lastOrDefault("end");

        }

        @Test(expected = AssertionError.class)
        public void testCompletedError() {
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onCompleted();
                    observer.onError(new Exception());
                    return Subscriptions.empty();
                }
            })).lastOrDefault("end");
        }

        @Test(expected = AssertionError.class)
        public void testCompletedNext() {
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onCompleted();
                    observer.onNext("one");
                    return Subscriptions.empty();
                }
            })).lastOrDefault("end");
        }

        @Test(expected = AssertionError.class)
        public void testErrorCompleted() {
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onError(new Exception());
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            })).lastOrDefault("end");
        }

        @Test(expected = AssertionError.class)
        public void testDoubleError() {
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onError(new Exception());
                    observer.onError(new Exception());
                    return Subscriptions.empty();
                }
            })).lastOrDefault("end");
        }

        @Test(expected = AssertionError.class)
        public void testErrorNext() {
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onError(new Exception());
                    observer.onNext("one");
                    return Subscriptions.empty();
                }
            })).lastOrDefault("end");
        }

        @Test
        public void testNextCompleted() {
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(Observer<String> observer)
                {
                    observer.onNext("one");
                    observer.onCompleted();
                    return Subscriptions.empty();
                }
            })).lastOrDefault("end");
        }

        @Test
        public void testConcurrentNextNext() {
            final List<Thread> threads = new ArrayList<Thread>();
            final AtomicReference<Throwable> threadFailure = new AtomicReference<Throwable>();
            Observable.create(assertTrustedObservable(new Func1<Observer<String>, Subscription>()
            {
                @Override
                public Subscription call(final Observer<String> observer)
                {
                    threads.add(new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            observer.onNext("one");
                        }
                    }));
                    threads.add(new Thread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            observer.onNext("two");
                        }
                    }));
                    return Subscriptions.empty();
                }
            })).subscribe(new SlowObserver());
            for (Thread thread : threads) {
                thread.setUncaughtExceptionHandler(new UncaughtExceptionHandler()
                {
                    @Override
                    public void uncaughtException(Thread thread, Throwable throwable)
                    {
                        threadFailure.set(throwable);
                    }
                });
                thread.start();
            }
            for (Thread thread : threads) {
                try {
                    thread.join();
                } catch (InterruptedException ignored) {
                }
            }
            // Junit seems pretty bad about exposing test failures inside of created threads.
            assertNotNull("exception thrown by thread", threadFailure.get());
            assertEquals("class of exception thrown by thread", AssertionError.class, threadFailure.get().getClass());
        }

        private static class SlowObserver implements Observer<String>
        {
            @Override
            public void onCompleted()
            {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }

            @Override
            public void onError(Exception e)
            {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }

            @Override
            public void onNext(String args)
            {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
