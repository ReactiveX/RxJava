package rx.concurrency;

import org.junit.Test;
import org.mockito.InOrder;
import rx.util.functions.Action0;

import static org.mockito.Mockito.*;

public class ImmediateSchedulerTest {
  @Test
  public void testNestedActions() {
    final ImmediateScheduler scheduler = new ImmediateScheduler();

    final Action0 firstStepStart = mock(Action0.class);
    final Action0 firstStepEnd = mock(Action0.class);

    final Action0 secondStepStart = mock(Action0.class);
    final Action0 secondStepEnd = mock(Action0.class);

    final Action0 thirdStepStart = mock(Action0.class);
    final Action0 thirdStepEnd = mock(Action0.class);

    final Action0 firstAction = new Action0() {
      @Override
      public void call() {
        firstStepStart.call();
        firstStepEnd.call();
      }
    };
    final Action0 secondAction = new Action0() {
      @Override
      public void call() {
        secondStepStart.call();
        scheduler.schedule(firstAction);
        secondStepEnd.call();

      }
    };
    final Action0 thirdAction = new Action0() {
      @Override
      public void call() {
        thirdStepStart.call();
        scheduler.schedule(secondAction);
        thirdStepEnd.call();
      }
    };

    InOrder inOrder = inOrder(firstStepStart, firstStepEnd, secondStepStart, secondStepEnd, thirdStepStart, thirdStepEnd);

    scheduler.schedule(thirdAction);

    inOrder.verify(thirdStepStart, times(1)).call();
    inOrder.verify(secondStepStart, times(1)).call();
    inOrder.verify(firstStepStart, times(1)).call();
    inOrder.verify(firstStepEnd, times(1)).call();
    inOrder.verify(secondStepEnd, times(1)).call();
    inOrder.verify(thirdStepEnd, times(1)).call();
  }
}
