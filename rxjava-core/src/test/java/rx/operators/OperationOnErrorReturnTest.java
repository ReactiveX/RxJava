package rx.operators;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationOnErrorReturn.*;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public class OperationOnErrorReturnTest {

    @Test
    public void testResumeNext() {
        Subscription s = mock(Subscription.class);
        TestObservable f = new TestObservable(s, "one");
        Observable<String> w = Observable.create(f);
        final AtomicReference<Throwable> capturedException = new AtomicReference<Throwable>();

        Observable<String> observable = Observable.create(onErrorReturn(w, new Func1<Throwable, String>() {

            @Override
            public String call(Throwable e) {
                capturedException.set(e);
                return "failure";
            }

        }));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        verify(aObserver, Mockito.never()).onError(any(Throwable.class));
        verify(aObserver, times(1)).onCompleted();
        verify(aObserver, times(1)).onNext("one");
        verify(aObserver, times(1)).onNext("failure");
        assertNotNull(capturedException.get());
    }

    /**
     * Test that when a function throws an exception this is propagated through onError
     */
    @Test
    public void testFunctionThrowsError() {
        Subscription s = mock(Subscription.class);
        TestObservable f = new TestObservable(s, "one");
        Observable<String> w = Observable.create(f);
        final AtomicReference<Throwable> capturedException = new AtomicReference<Throwable>();

        Observable<String> observable = Observable.create(onErrorReturn(w, new Func1<Throwable, String>() {

            @Override
            public String call(Throwable e) {
                capturedException.set(e);
                throw new RuntimeException("exception from function");
            }

        }));

        @SuppressWarnings("unchecked")
        Observer<String> aObserver = mock(Observer.class);
        observable.subscribe(aObserver);

        try {
            f.t.join();
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        // we should get the "one" value before the error
        verify(aObserver, times(1)).onNext("one");

        // we should have received an onError call on the Observer since the resume function threw an exception
        verify(aObserver, times(1)).onError(any(Throwable.class));
        verify(aObserver, times(0)).onCompleted();
        assertNotNull(capturedException.get());
    }

    private static class TestObservable implements Observable.OnSubscribeFunc<String> {

        final Subscription s;
        final String[] values;
        Thread t = null;

        public TestObservable(Subscription s, String... values) {
            this.s = s;
            this.values = values;
        }

        @Override
        public Subscription onSubscribe(final Observer<? super String> observer) {
            System.out.println("TestObservable subscribed to ...");
            t = new Thread(new Runnable() {

                @Override
                public void run() {
                    try {
                        System.out.println("running TestObservable thread");
                        for (String s : values) {
                            System.out.println("TestObservable onNext: " + s);
                            observer.onNext(s);
                        }
                        throw new RuntimeException("Forced Failure");
                    } catch (Throwable e) {
                        observer.onError(e);
                    }
                }

            });
            System.out.println("starting TestObservable thread");
            t.start();
            System.out.println("done starting TestObservable thread");
            return s;
        }
    }
}
