package rx.operators;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;

public class OperatorUnsubscribeOnTest {

    @Test
    public void testUnsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnSameThread() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
            Observable<Integer> w = Observable.create(new OnSubscribe<Integer>() {

                @Override
                public void call(Subscriber<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.add(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onCompleted();
                }
            });

            TestObserver<Integer> observer = new TestObserver<Integer>();
            w.subscribeOn(UI_EVENT_LOOP).observeOn(Schedulers.computation()).unsubscribeOn(UI_EVENT_LOOP).subscribe(observer);

            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertTrue(unsubscribeThread == UI_EVENT_LOOP.getThread());

            observer.assertReceivedOnNext(Arrays.asList(1, 2));
            observer.assertTerminalEvent();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    @Test
    public void testUnsubscribeWhenSubscribeOnAndUnsubscribeOnAreOnDifferentThreads() throws InterruptedException {
        UIEventLoopScheduler UI_EVENT_LOOP = new UIEventLoopScheduler();
        try {
            final ThreadSubscription subscription = new ThreadSubscription();
            final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
            Observable<Integer> w = Observable.create(new OnSubscribe<Integer>() {

                @Override
                public void call(Subscriber<? super Integer> t1) {
                    subscribeThread.set(Thread.currentThread());
                    t1.add(subscription);
                    t1.onNext(1);
                    t1.onNext(2);
                    t1.onCompleted();
                }
            });

            TestObserver<Integer> observer = new TestObserver<Integer>();
            w.subscribeOn(Schedulers.newThread()).observeOn(Schedulers.computation()).unsubscribeOn(UI_EVENT_LOOP).subscribe(observer);

            Thread unsubscribeThread = subscription.getThread();

            assertNotNull(unsubscribeThread);
            assertNotSame(Thread.currentThread(), unsubscribeThread);

            assertNotNull(subscribeThread.get());
            assertNotSame(Thread.currentThread(), subscribeThread.get());
            // True for Schedulers.newThread()

            System.out.println("unsubscribeThread: " + unsubscribeThread);
            System.out.println("subscribeThread.get(): " + subscribeThread.get());
            assertTrue(unsubscribeThread == UI_EVENT_LOOP.getThread());

            observer.assertReceivedOnNext(Arrays.asList(1, 2));
            observer.assertTerminalEvent();
        } finally {
            UI_EVENT_LOOP.shutdown();
        }
    }

    private static class ThreadSubscription implements Subscription {
        private volatile Thread thread;

        private final CountDownLatch latch = new CountDownLatch(1);

        private final Subscription s = Subscriptions.create(new Action0() {

            @Override
            public void call() {
                System.out.println("unsubscribe invoked: " + Thread.currentThread());
                thread = Thread.currentThread();
                latch.countDown();
            }

        });

        @Override
        public void unsubscribe() {
            s.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return s.isUnsubscribed();
        }

        public Thread getThread() throws InterruptedException {
            latch.await();
            return thread;
        }
    }

    public static class UIEventLoopScheduler extends Scheduler {

        private volatile Thread t;
        private final Inner inner;

        public UIEventLoopScheduler() {
            inner = Schedulers.newThread().createInner();
            /*
             * DON'T DO THIS IN PRODUCTION CODE
             */
            final CountDownLatch latch = new CountDownLatch(1);
            inner.schedule(new Action1<Recurse>() {

                @Override
                public void call(Recurse inner) {
                    t = Thread.currentThread();
                    latch.countDown();
                }

            });
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException("failed to initialize thread");
            }
        }

        public void shutdown() {
            inner.unsubscribe();
        }

        public Thread getThread() {
            return t;
        }

        @Override
        public Inner createInner() {
            // same one every time
            return inner;
        }

    }
}
