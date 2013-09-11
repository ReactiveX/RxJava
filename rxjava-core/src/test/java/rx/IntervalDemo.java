package rx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import rx.util.functions.Action0;
import rx.util.functions.Action1;

@Ignore // since this doesn't do any automatic testing
public class IntervalDemo {
	
	@Test public void demoInterval() throws Exception {
		testLongObservable(Observable.interval(500, TimeUnit.MILLISECONDS).take(4), "demoInterval");
	}	
	
	public void testLongObservable(Observable<Long> o, final String testname) throws Exception {
		final List<Long> l = new ArrayList<Long>();
		Action1<Long> onNext = new Action1<Long>() {
			public void call(Long i) { 
				l.add(i);
				System.out.println(testname + " got " + i);
			}
		};
		Action1<Throwable> onError = new Action1<Throwable>() {
			public void call(Throwable t) { t.printStackTrace(); }
		};
		Action0 onComplete = new Action0() {
			public void call() {
				System.out.println(testname + " complete"); 
			}
		};
		o.subscribe(onNext, onError, onComplete);
		
		// need to wait, otherwise JUnit kills the thread of interval()
		Thread.sleep(2500);
	}
	
}
