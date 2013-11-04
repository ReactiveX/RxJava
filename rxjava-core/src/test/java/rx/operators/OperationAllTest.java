package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

import static org.mockito.Mockito.*;
import static rx.operators.OperationAll.all;

public class OperationAllTest {

  @Test
  @SuppressWarnings("unchecked")
  public void testAll() {
    Observable<String> obs = Observable.from("one", "two", "six");

    Observer<Boolean> observer = mock(Observer.class);
    Observable.create(all(obs, new Func1<String, Boolean>() {
      @Override
      public Boolean call(String s) {
        return s.length() == 3;
      }
    })).subscribe(observer);

    verify(observer).onNext(true);
    verify(observer).onCompleted();
    verifyNoMoreInteractions(observer);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testNotAll() {
    Observable<String> obs = Observable.from("one", "two", "three", "six");

    Observer<Boolean> observer = mock(Observer.class);
    Observable.create(all(obs, new Func1<String, Boolean>() {
      @Override
      public Boolean call(String s) {
        return s.length() == 3;
      }
    })).subscribe(observer);

    verify(observer).onNext(false);
    verify(observer).onCompleted();
    verifyNoMoreInteractions(observer);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testEmpty() {
    Observable<String> obs = Observable.empty();

    Observer<Boolean> observer = mock(Observer.class);
    Observable.create(all(obs, new Func1<String, Boolean>() {
      @Override
      public Boolean call(String s) {
        return s.length() == 3;
      }
    })).subscribe(observer);

    verify(observer).onNext(true);
    verify(observer).onCompleted();
    verifyNoMoreInteractions(observer);
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testError() {
    Throwable error = new Throwable();
    Observable<String> obs = Observable.error(error);

    Observer<Boolean> observer = mock(Observer.class);
    Observable.create(all(obs, new Func1<String, Boolean>() {
      @Override
      public Boolean call(String s) {
        return s.length() == 3;
      }
    })).subscribe(observer);

    verify(observer).onError(error);
    verifyNoMoreInteractions(observer);
  }
}
