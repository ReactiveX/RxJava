package rx.operators;

import static org.junit.Assert.*;
import static rx.operators.OperationMaterialize.*;

import java.util.List;
import java.util.Vector;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;

public class OperationMaterializeTest {

    @Test
    public void testMaterialize1() {
        // null will cause onError to be triggered before "three" can be returned
        final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", null, "three");

        TestObserver Observer = new TestObserver();
        Observable<Notification<String>> m = Observable.create(materialize(Observable.create(o1)));
        m.subscribe(Observer);

        try {
            o1.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertFalse(Observer.onError);
        assertTrue(Observer.onCompleted);
        assertEquals(3, Observer.notifications.size());
        assertEquals("one", Observer.notifications.get(0).getValue());
        assertTrue(Observer.notifications.get(0).isOnNext());
        assertEquals("two", Observer.notifications.get(1).getValue());
        assertTrue(Observer.notifications.get(1).isOnNext());
        assertEquals(NullPointerException.class, Observer.notifications.get(2).getThrowable().getClass());
        assertTrue(Observer.notifications.get(2).isOnError());
    }

    @Test
    public void testMaterialize2() {
        final TestAsyncErrorObservable o1 = new TestAsyncErrorObservable("one", "two", "three");

        TestObserver Observer = new TestObserver();
        Observable<Notification<String>> m = Observable.create(materialize(Observable.create(o1)));
        m.subscribe(Observer);

        try {
            o1.t.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertFalse(Observer.onError);
        assertTrue(Observer.onCompleted);
        assertEquals(4, Observer.notifications.size());
        assertEquals("one", Observer.notifications.get(0).getValue());
        assertTrue(Observer.notifications.get(0).isOnNext());
        assertEquals("two", Observer.notifications.get(1).getValue());
        assertTrue(Observer.notifications.get(1).isOnNext());
        assertEquals("three", Observer.notifications.get(2).getValue());
        assertTrue(Observer.notifications.get(2).isOnNext());
        assertTrue(Observer.notifications.get(3).isOnCompleted());
    }

    @Test
    public void testMultipleSubscribes() throws InterruptedException, ExecutionException {
        final TestAsyncErrorObservable o = new TestAsyncErrorObservable("one", "two", null, "three");

        Observable<Notification<String>> m = Observable.create(materialize(Observable.create(o)));

        assertEquals(3, m.toList().toBlockingObservable().toFuture().get().size());
        assertEquals(3, m.toList().toBlockingObservable().toFuture().get().size());
    }

    private static class TestObserver implements Observer<Notification<String>> {

        boolean onCompleted = false;
        boolean onError = false;
        List<Notification<String>> notifications = new Vector<Notification<String>>();

        @Override
        public void onCompleted() {
            this.onCompleted = true;
        }

        @Override
        public void onError(Throwable e) {
            this.onError = true;
        }

        @Override
        public void onNext(Notification<String> value) {
            this.notifications.add(value);
        }

    }

    private static class TestAsyncErrorObservable implements Observable.OnSubscribeFunc<String> {

        String[] valuesToReturn;

        TestAsyncErrorObservable(String... values) {
            valuesToReturn = values;
        }

        volatile Thread t;

        @Override
        public Subscription onSubscribe(final Observer<? super String> observer) {
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    for (String s : valuesToReturn) {
                        if (s == null) {
                            System.out.println("throwing exception");
                            try {
                                Thread.sleep(100);
                            } catch (Throwable e) {

                            }
                            observer.onError(new NullPointerException());
                            return;
                        } else {
                            observer.onNext(s);
                        }
                    }
                    System.out.println("subscription complete");
                    observer.onCompleted();
                }

            });
            t.start();

            return new Subscription() {

                @Override
                public void unsubscribe() {

                }

            };
        }
    }
}
