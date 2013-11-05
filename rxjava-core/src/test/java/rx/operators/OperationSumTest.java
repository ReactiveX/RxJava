package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationSum.*;

import org.junit.Test;

import rx.Observable;
import rx.Observer;

public class OperationSumTest {

    @SuppressWarnings("unchecked")
    Observer<Integer> w = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Long> wl = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Float> wf = mock(Observer.class);
    @SuppressWarnings("unchecked")
    Observer<Double> wd = mock(Observer.class);

    @Test
    public void testSumOfAFewInts() throws Throwable {
        Observable<Integer> src = Observable.from(1, 2, 3, 4, 5);
        sum(src).subscribe(w);

        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(15);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testEmptySum() throws Throwable {
        Observable<Integer> src = Observable.empty();
        sum(src).subscribe(w);

        verify(w, times(1)).onNext(anyInt());
        verify(w).onNext(0);
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testSumOfAFewLongs() throws Throwable {
        Observable<Long> src = Observable.from(1L, 2L, 3L, 4L, 5L);
        sumLongs(src).subscribe(wl);

        verify(wl, times(1)).onNext(anyLong());
        verify(wl).onNext(15L);
        verify(wl, never()).onError(any(Throwable.class));
        verify(wl, times(1)).onCompleted();
    }

    @Test
    public void testEmptySumLongs() throws Throwable {
        Observable<Long> src = Observable.empty();
        sumLongs(src).subscribe(wl);

        verify(wl, times(1)).onNext(anyLong());
        verify(wl).onNext(0L);
        verify(wl, never()).onError(any(Throwable.class));
        verify(wl, times(1)).onCompleted();
    }

    @Test
    public void testSumOfAFewFloats() throws Throwable {
        Observable<Float> src = Observable.from(1.0f);
        sumFloats(src).subscribe(wf);

        verify(wf, times(1)).onNext(anyFloat());
        verify(wf).onNext(1.0f);
        verify(wf, never()).onError(any(Throwable.class));
        verify(wf, times(1)).onCompleted();
    }

    @Test
    public void testEmptySumFloats() throws Throwable {
        Observable<Float> src = Observable.empty();
        sumFloats(src).subscribe(wf);

        verify(wf, times(1)).onNext(anyFloat());
        verify(wf).onNext(0.0f);
        verify(wf, never()).onError(any(Throwable.class));
        verify(wf, times(1)).onCompleted();
    }

    @Test
    public void testSumOfAFewDoubles() throws Throwable {
        Observable<Double> src = Observable.from(0.0d, 1.0d, 0.5d);
        sumDoubles(src).subscribe(wd);

        verify(wd, times(1)).onNext(anyDouble());
        verify(wd).onNext(1.5d);
        verify(wd, never()).onError(any(Throwable.class));
        verify(wd, times(1)).onCompleted();
    }

    @Test
    public void testEmptySumDoubles() throws Throwable {
        Observable<Double> src = Observable.empty();
        sumDoubles(src).subscribe(wd);

        verify(wd, times(1)).onNext(anyDouble());
        verify(wd).onNext(0.0d);
        verify(wd, never()).onError(any(Throwable.class));
        verify(wd, times(1)).onCompleted();
    }
}
