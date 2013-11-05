package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.*;
import static rx.operators.OperationFirstOrDefault.*;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

public class OperationFirstOrDefaultTest {

    @Mock
    Observer<? super String> w;

    private static final Func1<String, Boolean> IS_D = new Func1<String, Boolean>() {
        @Override
        public Boolean call(String value) {
            return "d".equals(value);
        }
    };

    @Before
    public void before() {
        initMocks(this);
    }

    @Test
    public void testFirstOrElseOfNone() {
        Observable<String> src = Observable.empty();
        Observable.create(firstOrDefault(src, "default")).subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("default");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testFirstOrElseOfSome() {
        Observable<String> src = Observable.from("a", "b", "c");
        Observable.create(firstOrDefault(src, "default")).subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("a");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testFirstOrElseWithPredicateOfNoneMatchingThePredicate() {
        Observable<String> src = Observable.from("a", "b", "c");
        Observable.create(firstOrDefault(src, IS_D, "default")).subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("default");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }

    @Test
    public void testFirstOrElseWithPredicateOfSome() {
        Observable<String> src = Observable.from("a", "b", "c", "d", "e", "f");
        Observable.create(firstOrDefault(src, IS_D, "default")).subscribe(w);

        verify(w, times(1)).onNext(anyString());
        verify(w, times(1)).onNext("d");
        verify(w, never()).onError(any(Throwable.class));
        verify(w, times(1)).onCompleted();
    }
}
