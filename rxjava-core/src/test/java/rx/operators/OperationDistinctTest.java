package rx.operators;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.util.functions.Func1;

import java.util.Comparator;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.never;
import static org.mockito.MockitoAnnotations.initMocks;
import static rx.Observable.*;
import static rx.operators.OperationDistinct.distinct;

public class OperationDistinctTest {

  @Mock
  Observer<? super String> w;
  @Mock
  Observer<? super String> w2;

  // nulls lead to exceptions
  final Func1<String, String> TO_UPPER_WITH_EXCEPTION = new Func1<String, String>() {
    @Override
    public String call(String s) {
      if (s.equals("x")) {
        return "XX";
      }
      return s.toUpperCase();
    }
  };

  final Comparator<String> COMPARE_LENGTH = new Comparator<String>() {
    @Override
    public int compare(String s1, String s2) {
      return s1.length() - s2.length();
    }
  };

  @Before
  public void before() {
    initMocks(this);
  }

  @Test
  public void testDistinctOfNone() {
    Observable<String> src = empty();
    create(distinct(src)).subscribe(w);

    verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));
    verify(w, times(1)).onCompleted();
  }

  @Test
  public void testDistinctOfNoneWithKeySelector() {
    Observable<String> src = empty();
    create(distinct(src, TO_UPPER_WITH_EXCEPTION)).subscribe(w);

    verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));
    verify(w, times(1)).onCompleted();
  }

  @Test
  public void testDistinctOfNormalSource() {
    Observable<String> src = from("a", "b", "c", "c", "c", "b", "b", "a", "e");
    create(distinct(src)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext("a");
    inOrder.verify(w, times(1)).onNext("b");
    inOrder.verify(w, times(1)).onNext("c");
    inOrder.verify(w, times(1)).onNext("e");
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testDistinctOfNormalSourceWithKeySelector() {
    Observable<String> src = from("a", "B", "c", "C", "c", "B", "b", "a", "E");
    create(distinct(src, TO_UPPER_WITH_EXCEPTION)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext("a");
    inOrder.verify(w, times(1)).onNext("B");
    inOrder.verify(w, times(1)).onNext("c");
    inOrder.verify(w, times(1)).onNext("E");
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testDistinctOfNormalSourceWithComparator() {
    Observable<String> src = from("1", "12", "123", "aaa", "321", "12", "21", "1", "12345");
    create(distinct(src, COMPARE_LENGTH)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext("1");
    inOrder.verify(w, times(1)).onNext("12");
    inOrder.verify(w, times(1)).onNext("123");
    inOrder.verify(w, times(1)).onNext("12345");
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testDistinctOfNormalSourceWithKeySelectorAndComparator() {
    Observable<String> src = from("a", "x", "ab", "abc", "cba", "de", "x", "a", "abcd");
    create(distinct(src, TO_UPPER_WITH_EXCEPTION, COMPARE_LENGTH)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext("a");
    inOrder.verify(w, times(1)).onNext("x");
    inOrder.verify(w, times(1)).onNext("abc");
    inOrder.verify(w, times(1)).onNext("abcd");
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testDistinctOfNormalSourceWithKeySelectorAndComparatorAndTwoSubscriptions() {
    Observable<String> src = from("a", "x", "ab", "abc", "cba", "de", "x", "a", "abcd");
    create(distinct(src, TO_UPPER_WITH_EXCEPTION, COMPARE_LENGTH)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext("a");
    inOrder.verify(w, times(1)).onNext("x");
    create(distinct(src, TO_UPPER_WITH_EXCEPTION, COMPARE_LENGTH)).subscribe(w2);
    inOrder.verify(w, times(1)).onNext("abc");
    inOrder.verify(w, times(1)).onNext("abcd");
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));

    InOrder inOrder2 = inOrder(w2);
    inOrder2.verify(w2, times(1)).onNext("a");
    inOrder2.verify(w2, times(1)).onNext("x");
    inOrder2.verify(w2, times(1)).onNext("abc");
    inOrder2.verify(w2, times(1)).onNext("abcd");
    inOrder2.verify(w2, times(1)).onCompleted();
    inOrder2.verify(w2, never()).onNext(anyString());
    verify(w2, never()).onError(any(Throwable.class));
  }

  @Test
  public void testDistinctOfSourceWithNulls() {
    Observable<String> src = from(null, "a", "a", null, null, "b", null);
    create(distinct(src)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext(null);
    inOrder.verify(w, times(1)).onNext("a");
    inOrder.verify(w, times(1)).onNext("b");
    inOrder.verify(w, times(1)).onCompleted();
    inOrder.verify(w, never()).onNext(anyString());
    verify(w, never()).onError(any(Throwable.class));
  }

  @Test
  public void testDistinctOfSourceWithExceptionsFromKeySelector() {
    Observable<String> src = from("a", "b", null, "c");
    create(distinct(src, TO_UPPER_WITH_EXCEPTION)).subscribe(w);

    InOrder inOrder = inOrder(w);
    inOrder.verify(w, times(1)).onNext("a");
    inOrder.verify(w, times(1)).onNext("b");
    inOrder.verify(w, times(1)).onError(any(NullPointerException.class));
    inOrder.verify(w, never()).onNext(anyString());
    inOrder.verify(w, never()).onCompleted();
  }
}
