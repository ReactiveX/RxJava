package rx.internal.operators;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.functions.*;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

public class OperatorMapPairTest {
    @Test
    public void castCrashUnsubscribes() {
        
        PublishSubject<Integer> ps = PublishSubject.create();
        
        TestSubscriber<Integer> ts = TestSubscriber.create();
        
        ps.flatMap(new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t) {
                throw new TestException();
            }
        }, new Func2<Integer, Integer, Integer>() {
            @Override
            public Integer call(Integer t1, Integer t2) {
                return t1;
            }
        }).unsafeSubscribe(ts);
        
        Assert.assertTrue("Not subscribed?", ps.hasObservers());
        
        ps.onNext(1);
        
        Assert.assertFalse("Subscribed?", ps.hasObservers());
        
        ts.assertError(TestException.class);
    }
}
