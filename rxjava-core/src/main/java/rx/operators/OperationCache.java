package rx.operators;

import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public final class OperationCache
{
    public static <T> Func1<Observer<T>, Subscription> cache(Observable<T> source) {
        return new Cache<T>(source);
    }

    private static class Cache<T> implements Func1<Observer<T>, Subscription>, Observer<T>
    {

        private Observable<T> source;
        private boolean isSubscribed = false;
        private final Map<Subscription,Observer<T>> subscriptions = new HashMap<Subscription, Observer<T>>();
        private Exception exception = null;
        private final List<T> history = Collections.synchronizedList(new ArrayList<T>());

        public Cache(Observable<T> source)
        {
            this.source = source;
        }

        @Override
        public Subscription call(Observer<T> observer)
        {
            int item = 0;
            Subscription subscription;
            boolean needSubscribe;

            for (;;) {
                while (item < history.size()) {
                    observer.onNext(history.get(item++));
                }

                synchronized (subscriptions) {
                    if (item < history.size()) {
                        continue;
                    }

                    if (exception != null) {
                        observer.onError(exception);
                        return Subscriptions.empty();
                    }
                    if (source == null) {
                        observer.onCompleted();
                        return Subscriptions.empty();
                    }

                    subscription = new CacheSubscription();
                    subscriptions.put(subscription, observer);
                    needSubscribe = !isSubscribed;
                    if (needSubscribe) {
                        isSubscribed = true;
                    }
                    break;
                }
            }

            if (needSubscribe) {
                source.subscribe(this);
            }

            return subscription;
        }

        @Override
        public void onCompleted()
        {
            synchronized (subscriptions) {
                source = null;
                for (Observer<T> observer : new ArrayList<Observer<T>>(subscriptions.values())) {
                    observer.onCompleted();
                }
                subscriptions.clear();
            }
        }

        @Override
        public void onError(Exception e)
        {
            synchronized (subscriptions) {
                source = null;
                exception = e;
                for (Observer<T> observer : new ArrayList<Observer<T>>(subscriptions.values())) {
                    observer.onError(e);
                }
                subscriptions.clear();
            }
        }

        @Override
        public void onNext(T args)
        {
            synchronized (subscriptions) {
                history.add(args);
                for (Observer<T> observer : new ArrayList<Observer<T>>(subscriptions.values())) {
                    observer.onNext(args);
                }
            }
        }

        private class CacheSubscription implements Subscription
        {
            @Override
            public void unsubscribe()
            {
                synchronized (subscriptions) {
                    subscriptions.remove(this);
                }
            }
        }
    }

    public static class UnitTest {

        private final Exception testException = new Exception();

        @Test
        public void testNoSubscription() {
            final SynchronousObservableFunc synchronousObservableFunc = new SynchronousObservableFunc();
            Observable<String> cache = Observable.create(cache(Observable.create(synchronousObservableFunc)));
            assertFalse("Source observer subscribed", synchronousObservableFunc.isSubscribed.get());
        }

        @Test
        public void testSynchronous() {
            Observable<String> observable = Observable.create(cache(Observable.create(new SynchronousObservableFunc())));

            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            assertCompletedObserver(aObserver);

            aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            assertCompletedObserver(aObserver);
        }

        private void assertCompletedObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        private static class SynchronousObservableFunc implements Func1<Observer<String>, Subscription>
        {
            private AtomicBoolean isSubscribed = new AtomicBoolean(false);

            @Override
            public Subscription call(Observer<String> observer)
            {
                assertFalse("Source observer subscribed twice", isSubscribed.getAndSet(true));
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onCompleted();
                return Subscriptions.empty();
            }
        }

        @Test
        public void testSynchronousError() {
            Observable<String> observable = Observable.create(cache(Observable.create(new SynchronousObservableErrorFunc())));

            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            assertErrorObserver(aObserver);

            aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            assertErrorObserver(aObserver);
        }

        private void assertErrorObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, times(1)).onError(testException);
            verify(aObserver, Mockito.never()).onCompleted();
        }

        private class SynchronousObservableErrorFunc implements Func1<Observer<String>, Subscription>
        {
            private AtomicBoolean isSubscribed = new AtomicBoolean(false);

            @Override
            public Subscription call(Observer<String> observer)
            {
                assertFalse("Source observer subscribed twice", isSubscribed.getAndSet(true));
                observer.onNext("one");
                observer.onNext("two");
                observer.onNext("three");
                observer.onError(testException);
                return Subscriptions.empty();
            }
        }

        @Test
        public void testAsync() {
            AsyncObservableFunc asyncObservableFunc = new AsyncObservableFunc();
            Observable<String> observable = Observable.create(cache(Observable.create(asyncObservableFunc)));

            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            asyncObservableFunc.waitToFinish();
            assertCompletedObserver(aObserver);

            aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            assertCompletedObserver(aObserver);
        }

        private static class AsyncObservableFunc implements Func1<Observer<String>, Subscription>
        {
            private AtomicBoolean isSubscribed = new AtomicBoolean(false);
            Thread t;

            @Override
            public Subscription call(final Observer<String> observer)
            {
                assertFalse("Source observer subscribed twice", isSubscribed.getAndSet(true));
                t = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        try {
                            Thread.sleep(10);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        observer.onNext("one");
                        observer.onNext("two");
                        observer.onNext("three");
                        observer.onCompleted();
                    }
                });
                t.start();

                return Subscriptions.empty();
            }

            public void waitToFinish() {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Test
        public void testAsyncError() {
            AsyncObservableErrorFunc asyncObservableErrorFunc = new AsyncObservableErrorFunc();
            Observable<String> observable = Observable.create(cache(Observable.create(asyncObservableErrorFunc)));

            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            asyncObservableErrorFunc.waitToFinish();
            assertErrorObserver(aObserver);

            aObserver = mock(Observer.class);
            observable.subscribe(aObserver);
            assertErrorObserver(aObserver);
        }

        private class AsyncObservableErrorFunc implements Func1<Observer<String>, Subscription>
        {
            private AtomicBoolean isSubscribed = new AtomicBoolean(false);
            Thread t;

            @Override
            public Subscription call(final Observer<String> observer)
            {
                assertFalse("Source observer subscribed twice", isSubscribed.getAndSet(true));
                t = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        try {
                            Thread.sleep(10);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        observer.onNext("one");
                        observer.onNext("two");
                        observer.onNext("three");
                        observer.onError(testException);
                    }
                });
                t.start();

                return Subscriptions.empty();
            }

            public void waitToFinish() {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Test
        public void testSubscribeMidSequence() {
            LatchedObservableFunc latchedObservableFunc = new LatchedObservableFunc();
            Observable<String> observable = Observable.create(cache(Observable.create(latchedObservableFunc)));

            Observer<String> aObserver = mock(Observer.class);
            observable.subscribe(aObserver);

            latchedObservableFunc.waitToTwo();
            assertObservedUntilTwo(aObserver);

            Observer<String> anotherObserver = mock(Observer.class);
            observable.subscribe(anotherObserver);
            assertObservedUntilTwo(anotherObserver);

            latchedObservableFunc.waitToFinish();
            assertCompletedObserver(aObserver);
            assertCompletedObserver(anotherObserver);
        }

        @Test
        public void testUnsubscribeFirstObserver() {
            LatchedObservableFunc latchedObservableFunc = new LatchedObservableFunc();
            Observable<String> observable = Observable.create(cache(Observable.create(latchedObservableFunc)));

            Observer<String> aObserver = mock(Observer.class);
            Subscription subscription = observable.subscribe(aObserver);

            latchedObservableFunc.waitToTwo();

            subscription.unsubscribe();
            assertObservedUntilTwo(aObserver);

            Observer<String> anotherObserver = mock(Observer.class);
            observable.subscribe(anotherObserver);
            assertObservedUntilTwo(anotherObserver);

            latchedObservableFunc.waitToFinish();
            assertObservedUntilTwo(aObserver);
            assertCompletedObserver(anotherObserver);
        }

        private void assertObservedUntilTwo(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, Mockito.never()).onCompleted();
        }

        private static class LatchedObservableFunc implements Func1<Observer<String>, Subscription>
        {
            private AtomicBoolean isSubscribed = new AtomicBoolean(false);
            private Thread t;
            private final Object latch = new Object();


            @Override
            public Subscription call(final Observer<String> observer)
            {
                assertFalse("Source observer subscribed twice", isSubscribed.getAndSet(true));
                observer.onNext("one");
                t = new Thread(new Runnable() {
                    @Override
                    public void run()
                    {
                        try {
                            Thread.sleep(10);
                        }
                        catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                        observer.onNext("two");
                        synchronized (latch) {
                            latch.notifyAll();
                            try {
                                latch.wait();
                            }
                            catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        observer.onNext("three");
                        observer.onCompleted();
                    }
                });
                t.start();

                return Subscriptions.empty();
            }

            public void waitToTwo()
            {
                try {
                    synchronized (latch) {
                        latch.wait();
                    }
                }
                catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

            public void waitToFinish() {
                try {
                    synchronized (latch) {
                        latch.notifyAll();
                    }
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
