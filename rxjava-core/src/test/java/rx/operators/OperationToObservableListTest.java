package rx.operators;

import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static rx.operators.OperationToObservableList.toObservableList;

public class OperationToObservableListTest {

  @Test
  public void testList() {
    Observable<String> w = Observable.from("one", "two", "three");
    Observable<List<String>> observable = Observable.create(toObservableList(w));

    @SuppressWarnings("unchecked")
    Observer<List<String>> aObserver = mock(Observer.class);
    observable.subscribe(aObserver);
    verify(aObserver, times(1)).onNext(Arrays.asList("one", "two", "three"));
    verify(aObserver, Mockito.never()).onError(any(Throwable.class));
    verify(aObserver, times(1)).onCompleted();
  }

  @Test
  public void testListMultipleObservers() {
    Observable<String> w = Observable.from("one", "two", "three");
    Observable<List<String>> observable = Observable.create(toObservableList(w));

    @SuppressWarnings("unchecked")
    Observer<List<String>> o1 = mock(Observer.class);
    observable.subscribe(o1);

    @SuppressWarnings("unchecked")
    Observer<List<String>> o2 = mock(Observer.class);
    observable.subscribe(o2);

    List<String> expected = Arrays.asList("one", "two", "three");

    verify(o1, times(1)).onNext(expected);
    verify(o1, Mockito.never()).onError(any(Throwable.class));
    verify(o1, times(1)).onCompleted();

    verify(o2, times(1)).onNext(expected);
    verify(o2, Mockito.never()).onError(any(Throwable.class));
    verify(o2, times(1)).onCompleted();
  }
}
