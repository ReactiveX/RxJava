package rx.operators;

import org.junit.Test;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

import java.util.Iterator;

import static org.junit.Assert.*;
import static rx.operators.OperationMostRecent.mostRecent;

public class OperationMostRecentTest {


  @Test
  public void testMostRecent() {
    Subject<String, String> observable = PublishSubject.create();

    Iterator<String> it = mostRecent(observable, "default").iterator();

    assertTrue(it.hasNext());
    assertEquals("default", it.next());
    assertEquals("default", it.next());

    observable.onNext("one");
    assertTrue(it.hasNext());
    assertEquals("one", it.next());
    assertEquals("one", it.next());

    observable.onNext("two");
    assertTrue(it.hasNext());
    assertEquals("two", it.next());
    assertEquals("two", it.next());

    observable.onCompleted();
    assertFalse(it.hasNext());

  }

  @Test(expected = TestException.class)
  public void testMostRecentWithException() {
    Subject<String, String> observable = PublishSubject.create();

    Iterator<String> it = mostRecent(observable, "default").iterator();

    assertTrue(it.hasNext());
    assertEquals("default", it.next());
    assertEquals("default", it.next());

    observable.onError(new TestException());
    assertTrue(it.hasNext());

    it.next();
  }

  private static class TestException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
