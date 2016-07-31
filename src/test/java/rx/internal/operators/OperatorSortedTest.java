package rx.internal.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.functions.Func2;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class OperatorSortedTest {

    @Test
    public void testSortedList() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<Integer> observable = w.sorted();

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onNext(5);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSortedListWithCustomFunction() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<Integer> observable = w.sorted(new Func2<Integer, Integer, Integer>() {

            @Override
            public Integer call(Integer t1, Integer t2) {
                return t2 - t1;
            }

        });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(5);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(1);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSortedCustomComparator() {
        Observable<Integer> w = Observable.just(1, 3, 2, 5, 4);
        Observable<Integer> observable = w.sorted(new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1.compareTo(t2);
            }

        });

        @SuppressWarnings("unchecked")
        Observer<Integer> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, times(1)).onNext(1);
        verify(observer, times(1)).onNext(2);
        verify(observer, times(1)).onNext(3);
        verify(observer, times(1)).onNext(4);
        verify(observer, times(1)).onNext(5);
        verify(observer, never()).onError(any(Throwable.class));
        verify(observer, times(1)).onCompleted();
    }

    @Test
    public void testSortedWithNonComparable() {
        NonComparable n1 = new NonComparable(1,"a");
        NonComparable n2 = new NonComparable(2,"b");
        NonComparable n3 = new NonComparable(3,"c");
        Observable<NonComparable> w = Observable.just(n1,n2,n3);

        Observable<NonComparable> observable = w.sorted();

        @SuppressWarnings("unchecked")
        Observer<NonComparable> observer = mock(Observer.class);
        observable.subscribe(observer);
        verify(observer, never()).onNext(any(NonComparable.class));
        verify(observer, never()).onCompleted();
        verify(observer,times(1)).onError(any(ClassCastException.class));
    }

    private final class NonComparable{
        public int i;
        public String s;

        NonComparable(int i, String s){
            this.i = i;
            this.s = s;
        }
    }
}
