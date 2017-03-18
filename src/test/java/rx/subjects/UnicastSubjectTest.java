package rx.subjects;

import org.junit.Test;
import rx.functions.Action0;
import rx.observers.TestSubscriber;

public class UnicastSubjectTest {

    @Test
    public void testOneArgFactoryDelayError() throws Exception {
        TestSubscriber<Long> subscriber = TestSubscriber.<Long>create();
        UnicastSubject<Long> s = UnicastSubject.create(true);
        s.onNext(1L);
        s.onNext(2L);
        s.onError(new RuntimeException());
        s.subscribe(subscriber);
        subscriber.assertValueCount(2);
        subscriber.assertError(RuntimeException.class);
    }

    @Test
    public void testOneArgFactoryNoDelayError() throws Exception {
        TestSubscriber<Long> subscriber = TestSubscriber.<Long>create();
        UnicastSubject<Long> s = UnicastSubject.create(false);
        s.onNext(1L);
        s.onNext(2L);
        s.onError(new RuntimeException());
        s.subscribe(subscriber);
        subscriber.assertValueCount(0);
        subscriber.assertError(RuntimeException.class);
    }

    @Test
    public void testThreeArgsFactoryDelayError() throws Exception {
        TestSubscriber<Long> subscriber = TestSubscriber.<Long>create();
        UnicastSubject<Long> s = UnicastSubject.create(16, new NoopAction0(), true);
        s.onNext(1L);
        s.onNext(2L);
        s.onError(new RuntimeException());
        s.subscribe(subscriber);
        subscriber.assertValueCount(2);
        subscriber.assertError(RuntimeException.class);
    }

    @Test
    public void testThreeArgsFactoryNoDelayError() throws Exception {
        TestSubscriber<Long> subscriber = TestSubscriber.<Long>create();
        UnicastSubject<Long> s = UnicastSubject.create(16, new NoopAction0(), false);
        s.onNext(1L);
        s.onNext(2L);
        s.onError(new RuntimeException());
        s.subscribe(subscriber);
        subscriber.assertValueCount(0);
        subscriber.assertError(RuntimeException.class);
    }

    @Test
    public void testZeroArgsFactory() throws Exception {
        TestSubscriber<Long> subscriber = TestSubscriber.<Long>create();
        UnicastSubject<Long> s = UnicastSubject.create();
        s.onNext(1L);
        s.onNext(2L);
        s.onError(new RuntimeException());
        s.subscribe(subscriber);
        subscriber.assertValueCount(0);
        subscriber.assertError(RuntimeException.class);
    }

    @Test
    public void testOneArgFactory() throws Exception {
        TestSubscriber<Long> subscriber = TestSubscriber.<Long>create();
        UnicastSubject<Long> s = UnicastSubject.create(16);
        s.onNext(1L);
        s.onNext(2L);
        s.onError(new RuntimeException());
        s.subscribe(subscriber);
        subscriber.assertValueCount(0);
        subscriber.assertError(RuntimeException.class);
    }

    @Test
    public void testTwoArgsFactory() throws Exception {
        TestSubscriber<Long> subscriber = TestSubscriber.<Long>create();
        UnicastSubject<Long> s = UnicastSubject.create(16, new NoopAction0());
        s.onNext(1L);
        s.onNext(2L);
        s.onError(new RuntimeException());
        s.subscribe(subscriber);
        subscriber.assertValueCount(0);
        subscriber.assertError(RuntimeException.class);
    }



    private static final class NoopAction0 implements Action0 {

        @Override
        public void call() {
        }
    }
}
