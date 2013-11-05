package rx.operators;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;
import static rx.operators.OperationSkipWhile.*;

import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;
import rx.util.functions.Func2;

public class OperationSkipWhileTest {

  @SuppressWarnings("unchecked")
  Observer<Integer> w = mock(Observer.class);

  private static final Func1<Integer, Boolean> LESS_THAN_FIVE = new Func1<Integer, Boolean>() {
    @Override
    public Boolean call(Integer v) {
      if (v == 42) throw new RuntimeException("that's not the answer to everything!");
      return v < 5;
    }
  };

  private static final Func2<Integer, Integer, Boolean> INDEX_LESS_THAN_THREE = new Func2<Integer, Integer, Boolean>() {
    @Override
    public Boolean call(Integer value, Integer index) {
      return index < 3;
    }
  };

  @Test
  public void testSkipWithIndex() {
    Observable<Integer> src = Observable.from(1, 2, 3, 4, 5);
    Observable.create(skipWhileWithIndex(src, INDEX_LESS_THAN_THREE)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext(4);
    inOrder.verify(w, times(1)).onNext(5);
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testSkipEmpty() {
    Observable<Integer> src = Observable.empty();
    Observable.create(skipWhile(src, LESS_THAN_FIVE)).subscribe(w);
    verify(w, never()).onNext(anyInt());
    verify(w, never()).onError(any(Throwable.class));
    verify(w, times(1)).onCompleted();
  }

  @Test
  public void testSkipEverything() {
    Observable<Integer> src = Observable.from(1, 2, 3, 4, 3, 2, 1);
    Observable.create(skipWhile(src, LESS_THAN_FIVE)).subscribe(w);
    verify(w, never()).onNext(anyInt());
    verify(w, never()).onError(any(Throwable.class));
    verify(w, times(1)).onCompleted();
  }

  @Test
  public void testSkipNothing() {
    Observable<Integer> src = Observable.from(5, 3, 1);
    Observable.create(skipWhile(src, LESS_THAN_FIVE)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext(5);
    inOrder.verify(w, times(1)).onNext(3);
    inOrder.verify(w, times(1)).onNext(1);
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testSkipSome() {
    Observable<Integer> src = Observable.from(1, 2, 3, 4, 5, 3, 1, 5);
    Observable.create(skipWhile(src, LESS_THAN_FIVE)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext(5);
    inOrder.verify(w, times(1)).onNext(3);
    inOrder.verify(w, times(1)).onNext(1);
    inOrder.verify(w, times(1)).onNext(5);
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testSkipError() {
    Observable<Integer> src = Observable.from(1, 2, 42, 5, 3, 1);
    Observable.create(skipWhile(src, LESS_THAN_FIVE)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, never()).onNext(anyInt());
    inOrder.verify(w, never()).onCompleted();
    inOrder.verify(w, times(1)).onError(any(RuntimeException.class));
  }
}
