package io.reactivex.internal.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Test;
import org.reactivestreams.Subscriber;

import io.reactivex.*;
import io.reactivex.exceptions.TestException;
import io.reactivex.subscribers.TestSubscriber;

public class OperatorDematerializeTest {

    @Test
    public void testDematerialize1() {
        Observable<Try<Optional<Integer>>> notifications = Observable.just(1, 2).materialize();
        Observable<Integer> dematerialize = notifications.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onComplete();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testDematerialize2() {
        Throwable exception = new Throwable("test");
        Observable<Integer> observable = Observable.error(exception);
        Observable<Integer> dematerialize = observable.materialize().dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testDematerialize3() {
        Exception exception = new Exception("test");
        Observable<Integer> observable = Observable.error(exception);
        Observable<Integer> dematerialize = observable.materialize().dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testErrorPassThru() {
        Exception exception = new Exception("test");
        Observable<Integer> observable = Observable.error(exception);
        Observable<Integer> dematerialize = observable.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();

        dematerialize.subscribe(observer);

        verify(observer, times(1)).onError(exception);
        verify(observer, times(0)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testCompletePassThru() {
        Observable<Integer> observable = Observable.empty();
        Observable<Integer> dematerialize = observable.dematerialize();

        Subscriber<Integer> observer = TestHelper.mockSubscriber();
        
        TestSubscriber<Integer> ts = new TestSubscriber<>(observer);
        dematerialize.subscribe(ts);

        System.out.println(ts.errors());

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onComplete();
        verify(observer, times(0)).onNext(any(Integer.class));
    }

    @Test
    public void testHonorsContractWhenCompleted() {
        Observable<Integer> source = Observable.just(1);
        
        Observable<Integer> result = source.materialize().dematerialize();
        
        Subscriber<Integer> o = TestHelper.mockSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o).onNext(1);
        verify(o).onComplete();
        verify(o, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testHonorsContractWhenThrows() {
        Observable<Integer> source = Observable.error(new TestException());
        
        Observable<Integer> result = source.materialize().dematerialize();
        
        Subscriber<Integer> o = TestHelper.mockSubscriber();
        
        result.unsafeSubscribe(o);
        
        verify(o, never()).onNext(any(Integer.class));
        verify(o, never()).onComplete();
        verify(o).onError(any(TestException.class));
    }
}