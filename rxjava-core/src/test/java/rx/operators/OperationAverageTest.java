package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationAverage.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;

public class OperationAverageTest {

    @SuppressWarnings("unchecked")
    Observer<Integer> w = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Long> wl = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Float> wf = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Double> wd = mock(Observer.class);

    @Test
    public void testAverageOfAFewInts() throws Throwable {
        Observable<Integer> src = Observable.from(1, 2, 3, 4, 6);
        average(src).subscribe(w);

        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(3);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverage() throws Throwable {
        Observable<Integer> src = Observable.empty();
        average(src).subscribe(w);

        verify(w, never()).onNext(anyInt());
        verify(w, times(1)).onError(any(ArithmeticException.class));
        verify(w, never()).onCompleted();
    }

    @Test
    public void testAverageOfAFewLongs() throws Throwable {
        Observable<Long> src = Observable.from(1L, 2L, 3L, 4L, 6L);
        averageLongs(src).subscribe(wl);

        verify(wl, times(1)).onNext(anyLong());
        verify(wl).onNext(3L);
        verify(wl, never()).onError(any(Throwable.class));
        verify(wl, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverageLongs() throws Throwable {
        Observable<Long> src = Observable.empty();
        averageLongs(src).subscribe(wl);

        verify(wl, never()).onNext(anyLong());
        verify(wl, times(1)).onError(any(ArithmeticException.class));
        verify(wl, never()).onCompleted();
    }

    @Test
    public void testAverageOfAFewFloats() throws Throwable {
        Observable<Float> src = Observable.from(1.0f, 2.0f);
        averageFloats(src).subscribe(wf);

        verify(wf, times(1)).onNext(anyFloat());
        verify(wf).onNext(1.5f);
        verify(wf, never()).onError(any(Throwable.class));
        verify(wf, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverageFloats() throws Throwable {
        Observable<Float> src = Observable.empty();
        averageFloats(src).subscribe(wf);

        verify(wf, never()).onNext(anyFloat());
        verify(wf, times(1)).onError(any(ArithmeticException.class));
        verify(wf, never()).onCompleted();
    }

    @Test
    public void testAverageOfAFewDoubles() throws Throwable {
        Observable<Double> src = Observable.from(1.0d, 2.0d);
        averageDoubles(src).subscribe(wd);

        verify(wd, times(1)).onNext(anyDouble());
        verify(wd).onNext(1.5d);
        verify(wd, never()).onError(any(Throwable.class));
        verify(wd, times(1)).onCompleted();
    }

    @Test
    public void testEmptyAverageDoubles() throws Throwable {
        Observable<Double> src = Observable.empty();
        averageDoubles(src).subscribe(wd);

        verify(wd, never()).onNext(anyDouble());
        verify(wd, times(1)).onError(any(ArithmeticException.class));
        verify(wd, never()).onCompleted();
    }
}
