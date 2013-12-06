package rx.operators;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import rx.Observable;
import rx.Observer;
import rx.concurrency.TestScheduler;
import rx.util.functions.Func1;

public class OperationDelayTest {
    @Mock
    private Observer<Long> observer;
    @Mock
    private Observer<Long> observer2;

    private TestScheduler scheduler;

    @Before
    public void before() {
        initMocks(this);
        scheduler = new TestScheduler();
    }
    
    @Test
    public void testDelay() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = OperationDelay.delay(source, 500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);
        
        InOrder inOrder = inOrder(observer);
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        
        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(2400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        
        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        
        scheduler.advanceTimeTo(3400L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        verify(observer, times(1)).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }

    @Test
    public void testLongDelay() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = OperationDelay.delay(source, 5L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);
        
        InOrder inOrder = inOrder(observer);
        
        scheduler.advanceTimeTo(5999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        
        scheduler.advanceTimeTo(6000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        scheduler.advanceTimeTo(6999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(7000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        scheduler.advanceTimeTo(7999L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        scheduler.advanceTimeTo(8000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
    }
    
    @Test
    public void testDelayWithError() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).map(new Func1<Long, Long>() {
            @Override
            public Long call(Long value) {
                if (value == 1L) {
                    throw new RuntimeException("error!");
                }
                return value; 
            }
        });
        Observable<Long> delayed = OperationDelay.delay(source, 1L, TimeUnit.SECONDS, scheduler);
        delayed.subscribe(observer);

        InOrder inOrder = inOrder(observer);
        
        scheduler.advanceTimeTo(1999L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        verify(observer, never()).onError(any(Throwable.class));
        
        scheduler.advanceTimeTo(2000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onError(any(Throwable.class));
        inOrder.verify(observer, never()).onNext(anyLong());
        verify(observer, never()).onCompleted();
        
        scheduler.advanceTimeTo(5000L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder.verify(observer, never()).onError(any(Throwable.class));
        verify(observer, never()).onCompleted();
    }
    
    // TODO activate this test once https://github.com/Netflix/RxJava/issues/552 is fixed
    @Ignore
    @Test
    public void testDelayWithMultipleSubscriptions() {
        Observable<Long> source = Observable.interval(1L, TimeUnit.SECONDS, scheduler).take(3);
        Observable<Long> delayed = OperationDelay.delay(source, 500L, TimeUnit.MILLISECONDS, scheduler);
        delayed.subscribe(observer);
        delayed.subscribe(observer2);
        
        InOrder inOrder = inOrder(observer);
        InOrder inOrder2 = inOrder(observer2);
        
        scheduler.advanceTimeTo(1499L, TimeUnit.MILLISECONDS);
        verify(observer, never()).onNext(anyLong());
        verify(observer2, never()).onNext(anyLong());

        scheduler.advanceTimeTo(1500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(0L);
        inOrder2.verify(observer2, times(1)).onNext(0L);

        scheduler.advanceTimeTo(2499L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());
        
        scheduler.advanceTimeTo(2500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(1L);
        inOrder2.verify(observer2, times(1)).onNext(1L);

        verify(observer, never()).onCompleted();
        verify(observer2, never()).onCompleted();

        scheduler.advanceTimeTo(3500L, TimeUnit.MILLISECONDS);
        inOrder.verify(observer, times(1)).onNext(2L);
        inOrder2.verify(observer2, times(1)).onNext(2L);
        inOrder.verify(observer, never()).onNext(anyLong());
        inOrder2.verify(observer2, never()).onNext(anyLong());
        inOrder.verify(observer, times(1)).onCompleted();
        inOrder2.verify(observer2, times(1)).onCompleted();

        verify(observer, never()).onError(any(Throwable.class));
        verify(observer2, never()).onError(any(Throwable.class));
    }
}
