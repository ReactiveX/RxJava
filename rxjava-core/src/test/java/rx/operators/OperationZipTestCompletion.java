package rx.operators;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;
import rx.util.functions.Func2;

/**
 * Systematically tests that when zipping an infinite and a finite Observable, 
 * the resulting Observable is finite.
 *
 */
public class OperationZipTestCompletion {
	Func2<String, String, String> concat2Strings;
	
	PublishSubject<String> s1;
	PublishSubject<String> s2;
	Observable<String> zipped;
	
	Observer<String> observer;
	InOrder inOrder;


	@Before @SuppressWarnings("unchecked")
	public void setUp() {
		concat2Strings = new Func2<String, String, String>() {
            @Override
            public String call(String t1, String t2) {
                return t1 + "-" + t2;
            }
        }; 
        
		s1 = PublishSubject.create();
		s2 = PublishSubject.create();
		zipped = Observable.zip(s1, s2, concat2Strings);
		
		observer = mock(Observer.class);
		inOrder = inOrder(observer);
		
		zipped.subscribe(observer);
	}
	
	@Test
	public void testFirstCompletesThenSecondInfinite() {
		s1.onNext("a");
		s1.onNext("b");
		s1.onCompleted();
		s2.onNext("1");
		inOrder.verify(observer, times(1)).onNext("a-1");
		s2.onNext("2");
		inOrder.verify(observer, times(1)).onNext("b-2");
		inOrder.verify(observer, times(1)).onCompleted();
		inOrder.verifyNoMoreInteractions();
	}
	
	@Test
	public void testSecondInfiniteThenFirstCompletes() {
		s2.onNext("1");
		s2.onNext("2");
		s1.onNext("a");
		inOrder.verify(observer, times(1)).onNext("a-1");
		s1.onNext("b");
		inOrder.verify(observer, times(1)).onNext("b-2");
		s1.onCompleted();
		inOrder.verify(observer, times(1)).onCompleted();
		inOrder.verifyNoMoreInteractions();
	}
	
	@Test
	public void testSecondCompletesThenFirstInfinite() {
		s2.onNext("1");
		s2.onNext("2");
		s2.onCompleted();
		s1.onNext("a");
		inOrder.verify(observer, times(1)).onNext("a-1");
		s1.onNext("b");
		inOrder.verify(observer, times(1)).onNext("b-2");
		inOrder.verify(observer, times(1)).onCompleted();
		inOrder.verifyNoMoreInteractions();
	}

	@Test
	public void testFirstInfiniteThenSecondCompletes() {
		s1.onNext("a");
		s1.onNext("b");
		s2.onNext("1");
		inOrder.verify(observer, times(1)).onNext("a-1");
		s2.onNext("2");
		inOrder.verify(observer, times(1)).onNext("b-2");
		s2.onCompleted();
		inOrder.verify(observer, times(1)).onCompleted();
		inOrder.verifyNoMoreInteractions();
	}

}
