package rx.operators;

import org.junit.Test;
import rx.Observer;
import rx.Subscription;

import java.util.concurrent.Future;

import static org.mockito.Mockito.*;
import static rx.operators.OperationToObservableFuture.ToObservableFuture;

public class OperationToObservableFutureTest {

  @Test
  public void testSuccess() throws Exception {
    Future<Object> future = mock(Future.class);
    Object value = new Object();
    when(future.get()).thenReturn(value);
    ToObservableFuture<Object> ob = new ToObservableFuture<Object>(future);
    Observer<Object> o = mock(Observer.class);

    Subscription sub = ob.onSubscribe(o);
    sub.unsubscribe();

    verify(o, times(1)).onNext(value);
    verify(o, times(1)).onCompleted();
    verify(o, never()).onError(null);
    verify(future, never()).cancel(true);
  }

  @Test
  public void testFailure() throws Exception {
    Future<Object> future = mock(Future.class);
    RuntimeException e = new RuntimeException();
    when(future.get()).thenThrow(e);
    ToObservableFuture<Object> ob = new ToObservableFuture<Object>(future);
    Observer<Object> o = mock(Observer.class);

    Subscription sub = ob.onSubscribe(o);
    sub.unsubscribe();

    verify(o, never()).onNext(null);
    verify(o, never()).onCompleted();
    verify(o, times(1)).onError(e);
    verify(future, never()).cancel(true);
  }
}
