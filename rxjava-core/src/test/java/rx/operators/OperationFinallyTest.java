package rx.operators;

import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Action0;

import static org.mockito.Mockito.*;
import static rx.operators.OperationFinally.finallyDo;

public class OperationFinallyTest {

  private Action0 aAction0;
  private Observer<String> aObserver;

  @SuppressWarnings("unchecked") // mocking has to be unchecked, unfortunately
  @Before
  public void before() {
    aAction0 = mock(Action0.class);
    aObserver = mock(Observer.class);
  }

  private void checkActionCalled(Observable<String> input) {
    Observable.create(finallyDo(input, aAction0)).subscribe(aObserver);
    verify(aAction0, times(1)).call();
  }

  @Test
  public void testFinallyCalledOnComplete() {
    checkActionCalled(Observable.from(new String[]{"1", "2", "3"}));
  }

  @Test
  public void testFinallyCalledOnError() {
    checkActionCalled(Observable.<String>error(new RuntimeException("expected")));
  }
}
