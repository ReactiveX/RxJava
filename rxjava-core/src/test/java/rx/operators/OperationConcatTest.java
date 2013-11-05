package rx.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationConcat.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.BooleanSubscription;

public class OperationConcatTest {

    @Test
    public void testConcat() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.from(o);
        final Observable<String> even = Observable.from(e);

        @SuppressWarnings("unchecked")
        Observable<String> concat = Observable.create(concat(odds, even));
        concat.subscribe(observer);

        verify(observer, times(7)).onNext(anyString());
    }

    @Test
    public void testConcatWithList() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.from(o);
        final Observable<String> even = Observable.from(e);
        final List<Observable<String>> list = new ArrayList<Observable<String>>();
        list.add(odds);
        list.add(even);
        Observable<String> concat = Observable.create(concat(list));
        concat.subscribe(observer);

        verify(observer, times(7)).onNext(anyString());
    }

    @Test
    public void testConcatObservableOfObservables() {
        @SuppressWarnings("unchecked")
        Observer<String> observer = mock(Observer.class);

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };

        final Observable<String> odds = Observable.from(o);
        final Observable<String> even = Observable.from(e);

        Observable<Observable<String>> observableOfObservables = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {

            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                // simulate what would happen in an observable
                observer.onNext(odds);
                observer.onNext(even);
                observer.onCompleted();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                        // unregister ... will never be called here since we are executing synchronously
                    }

                };
            }

        });
        Observable<String> concat = Observable.create(concat(observableOfObservables));

        concat.subscribe(observer);

        verify(observer, times(7)).onNext(anyString());
    }

    /**
     * Simple concat of 2 asynchronous observables ensuring it emits in correct order.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testSimpleAsyncConcat() {
        Observer<String> observer = mock(Observer.class);

        TestObservable<String> o1 = new TestObservable<String>("one", "two", "three");
        TestObservable<String> o2 = new TestObservable<String>("four", "five", "six");

        Observable.concat(Observable.create(o1), Observable.create(o2)).subscribe(observer);

        try {
            // wait for async observables to complete
            o1.t.join();
            o2.t.join();
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads");
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onNext("five");
        inOrder.verify(observer, times(1)).onNext("six");
    }

    /**
     * Test an async Observable that emits more async Observables
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testNestedAsyncConcat() throws Throwable {
        Observer<String> observer = mock(Observer.class);

        final TestObservable<String> o1 = new TestObservable<String>("one", "two", "three");
        final TestObservable<String> o2 = new TestObservable<String>("four", "five", "six");
        final TestObservable<String> o3 = new TestObservable<String>("seven", "eight", "nine");
        final CountDownLatch allowThird = new CountDownLatch(1);

        final AtomicReference<Thread> parent = new AtomicReference<Thread>();
        Observable<Observable<String>> observableOfObservables = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {

            @Override
            public Subscription onSubscribe(final Observer<? super Observable<String>> observer) {
                final BooleanSubscription s = new BooleanSubscription();
                parent.set(new Thread(new Runnable() {

                    @Override
                    public void run() {
                        try {
                            // emit first
                            if (!s.isUnsubscribed()) {
                                System.out.println("Emit o1");
                                observer.onNext(Observable.create(o1));
                            }
                            // emit second
                            if (!s.isUnsubscribed()) {
                                System.out.println("Emit o2");
                                observer.onNext(Observable.create(o2));
                            }

                            // wait until sometime later and emit third
                            try {
                                allowThird.await();
                            } catch (InterruptedException e) {
                                observer.onError(e);
                            }
                            if (!s.isUnsubscribed()) {
                                System.out.println("Emit o3");
                                observer.onNext(Observable.create(o3));
                            }

                        } catch (Throwable e) {
                            observer.onError(e);
                        } finally {
                            System.out.println("Done parent Observable");
                            observer.onCompleted();
                        }
                    }
                }));
                parent.get().start();
                return s;
            }
        });

        Observable.create(concat(observableOfObservables)).subscribe(observer);

        // wait for parent to start
        while (parent.get() == null) {
            Thread.sleep(1);
        }

        try {
            // wait for first 2 async observables to complete
            while (o1.t == null) {
                Thread.sleep(1);
            }
            System.out.println("Thread1 started ... waiting for it to complete ...");
            o1.t.join();
            while (o2.t == null) {
                Thread.sleep(1);
            }
            System.out.println("Thread2 started ... waiting for it to complete ...");
            o2.t.join();
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads", e);
        }

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(1)).onNext("one");
        inOrder.verify(observer, times(1)).onNext("two");
        inOrder.verify(observer, times(1)).onNext("three");
        inOrder.verify(observer, times(1)).onNext("four");
        inOrder.verify(observer, times(1)).onNext("five");
        inOrder.verify(observer, times(1)).onNext("six");
        // we shouldn't have the following 3 yet
        inOrder.verify(observer, never()).onNext("seven");
        inOrder.verify(observer, never()).onNext("eight");
        inOrder.verify(observer, never()).onNext("nine");
        // we should not be completed yet
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        // now allow the third
        allowThird.countDown();

        try {
            while (o3.t == null) {
                Thread.sleep(1);
            }
            // wait for 3rd to complete
            o3.t.join();
        } catch (Throwable e) {
            throw new RuntimeException("failed waiting on threads", e);
        }

        inOrder.verify(observer, times(1)).onNext("seven");
        inOrder.verify(observer, times(1)).onNext("eight");
        inOrder.verify(observer, times(1)).onNext("nine");

        inOrder.verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBlockedObservableOfObservables() {
        Observer<String> observer = mock(Observer.class);

        final String[] o = { "1", "3", "5", "7" };
        final String[] e = { "2", "4", "6" };
        final Observable<String> odds = Observable.from(o);
        final Observable<String> even = Observable.from(e);
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(callOnce, okToContinue, odds, even);
        Observable.OnSubscribeFunc<String> concatF = concat(Observable.create(observableOfObservables));
        Observable<String> concat = Observable.create(concatF);
        concat.subscribe(observer);
        try {
            //Block main thread to allow observables to serve up o1.
            callOnce.await();
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        // The concated observable should have served up all of the odds.
        verify(observer, times(1)).onNext("1");
        verify(observer, times(1)).onNext("3");
        verify(observer, times(1)).onNext("5");
        verify(observer, times(1)).onNext("7");

        try {
            // unblock observables so it can serve up o2 and complete
            okToContinue.countDown();
            observableOfObservables.t.join();
        } catch (Throwable ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
        // The concatenated observable should now have served up all the evens.
        verify(observer, times(1)).onNext("2");
        verify(observer, times(1)).onNext("4");
        verify(observer, times(1)).onNext("6");
    }

    @Test
    public void testConcatConcurrentWithInfinity() {
        final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
        //This observable will send "hello" MAX_VALUE time.
        final TestObservable<String> w2 = new TestObservable<String>("hello", Integer.MAX_VALUE);

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        @SuppressWarnings("unchecked")
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(Observable.create(w1), Observable.create(w2));
        Observable.OnSubscribeFunc<String> concatF = concat(Observable.create(observableOfObservables));

        Observable<String> concat = Observable.create(concatF);

        concat.take(50).subscribe(aObserver);

        //Wait for the thread to start up.
        try {
            Thread.sleep(25);
            w1.t.join();
            w2.t.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        InOrder inOrder = inOrder(aObserver);
        inOrder.verify(aObserver, times(1)).onNext("one");
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        inOrder.verify(aObserver, times(47)).onNext("hello");
        verify(aObserver, times(1)).onCompleted();
        verify(aObserver, never()).onError(any(Throwable.class));
    }

    @Test
    public void testConcatNonBlockingObservables() {

        final CountDownLatch okToContinueW1 = new CountDownLatch(1);
        final CountDownLatch okToContinueW2 = new CountDownLatch(1);

        final TestObservable<String> w1 = new TestObservable<String>(null, okToContinueW1, "one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<String>(null, okToContinueW2, "four", "five", "six");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        Observable<Observable<String>> observableOfObservables = Observable.create(new Observable.OnSubscribeFunc<Observable<String>>() {

            @Override
            public Subscription onSubscribe(Observer<? super Observable<String>> observer) {
                // simulate what would happen in an observable
                observer.onNext(Observable.create(w1));
                observer.onNext(Observable.create(w2));
                observer.onCompleted();

                return new Subscription() {

                    @Override
                    public void unsubscribe() {
                    }

                };
            }

        });
        Observable<String> concat = Observable.create(concat(observableOfObservables));
        concat.subscribe(aObserver);

        verify(aObserver, times(0)).onCompleted();

        try {
            // release both threads
            okToContinueW1.countDown();
            okToContinueW2.countDown();
            // wait for both to finish
            w1.t.join();
            w2.t.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        InOrder inOrder = inOrder(aObserver);
        inOrder.verify(aObserver, times(1)).onNext("one");
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        inOrder.verify(aObserver, times(1)).onNext("four");
        inOrder.verify(aObserver, times(1)).onNext("five");
        inOrder.verify(aObserver, times(1)).onNext("six");
        verify(aObserver, times(1)).onCompleted();

    }

    /**
     * Test unsubscribing the concatenated Observable in a single thread.
     */
    @Test
    public void testConcatUnsubscribe() {
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<String>(callOnce, okToContinue, "four", "five", "six");

        @SuppressWarnings("unchecked")
        final Observer<String> aObserver = mock(Observer.class);
        @SuppressWarnings("unchecked")
        final Observable<String> concat = Observable.create(concat(Observable.create(w1), Observable.create(w2)));
        final SafeObservableSubscription s1 = new SafeObservableSubscription();

        try {
            // Subscribe
            s1.wrap(concat.subscribe(aObserver));
            //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext once.
            callOnce.await();
            // Unsubcribe
            s1.unsubscribe();
            //Unblock the observable to continue.
            okToContinue.countDown();
            w1.t.join();
            w2.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        InOrder inOrder = inOrder(aObserver);
        inOrder.verify(aObserver, times(1)).onNext("one");
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        inOrder.verify(aObserver, times(1)).onNext("four");
        inOrder.verify(aObserver, never()).onNext("five");
        inOrder.verify(aObserver, never()).onNext("six");
        inOrder.verify(aObserver, never()).onCompleted();

    }

    /**
     * All observables will be running in different threads so subscribe() is unblocked. CountDownLatch is only used in order to call unsubscribe() in a predictable manner.
     */
    @Test
    public void testConcatUnsubscribeConcurrent() {
        final CountDownLatch callOnce = new CountDownLatch(1);
        final CountDownLatch okToContinue = new CountDownLatch(1);
        final TestObservable<String> w1 = new TestObservable<String>("one", "two", "three");
        final TestObservable<String> w2 = new TestObservable<String>(callOnce, okToContinue, "four", "five", "six");

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        @SuppressWarnings("unchecked")
        TestObservable<Observable<String>> observableOfObservables = new TestObservable<Observable<String>>(Observable.create(w1), Observable.create(w2));
        Observable.OnSubscribeFunc<String> concatF = concat(Observable.create(observableOfObservables));

        Observable<String> concat = Observable.create(concatF);

        Subscription s1 = concat.subscribe(aObserver);

        try {
            //Block main thread to allow observable "w1" to complete and observable "w2" to call onNext exactly once.
            callOnce.await();
            //"four" from w2 has been processed by onNext()
            s1.unsubscribe();
            //"five" and "six" will NOT be processed by onNext()
            //Unblock the observable to continue.
            okToContinue.countDown();
            w1.t.join();
            w2.t.join();
        } catch (Throwable e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        InOrder inOrder = inOrder(aObserver);
        inOrder.verify(aObserver, times(1)).onNext("one");
        inOrder.verify(aObserver, times(1)).onNext("two");
        inOrder.verify(aObserver, times(1)).onNext("three");
        inOrder.verify(aObserver, times(1)).onNext("four");
        inOrder.verify(aObserver, never()).onNext("five");
        inOrder.verify(aObserver, never()).onNext("six");
        verify(aObserver, never()).onCompleted();
        verify(aObserver, never()).onError(any(Throwable.class));
    }

    private static class TestObservable<T> implements Observable.OnSubscribeFunc<T> {

        private final Subscription s = new Subscription() {

            @Override
            public void unsubscribe() {
                subscribed = false;
            }

        };
        private final List<T> values;
        private Thread t = null;
        private int count = 0;
        private boolean subscribed = true;
        private final CountDownLatch once;
        private final CountDownLatch okToContinue;
        private final T seed;
        private final int size;

        public TestObservable(T... values) {
            this(null, null, values);
        }

        public TestObservable(CountDownLatch once, CountDownLatch okToContinue, T... values) {
            this.values = Arrays.asList(values);
            this.size = this.values.size();
            this.once = once;
            this.okToContinue = okToContinue;
            this.seed = null;
        }

        public TestObservable(T seed, int size) {
            values = null;
            once = null;
            okToContinue = null;
            this.seed = seed;
            this.size = size;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super T> observer) {
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        while (count < size && subscribed) {
                            if (null != values)
                                observer.onNext(values.get(count));
                            else
                                observer.onNext(seed);
                            count++;
                            //Unblock the main thread to call unsubscribe.
                            if (null != once)
                                once.countDown();
                            //Block until the main thread has called unsubscribe.
                            if (null != okToContinue)
                                okToContinue.await(5, TimeUnit.SECONDS);
                        }
                        if (subscribed)
                            observer.onCompleted();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        fail(e.getMessage());
                    }
                }

            });
            t.start();
            return s;
        }
    }
}
