package rx.internal.producers;

import org.junit.*;

import rx.Observable;
import rx.exceptions.TestException;
import rx.observers.*;

public class SingleProducerTest {

    @Test
    public void negativeRequestThrows() {
        SingleProducer<Integer> pa = new SingleProducer<Integer>(Subscribers.empty(), 1);
        try {
            pa.request(-99);
            Assert.fail("Failed to throw on invalid request amount");
        } catch (IllegalArgumentException ex) {
            Assert.assertEquals("n >= 0 required", ex.getMessage());
        }
    }

    @Test
    public void cancelBeforeOnNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        SingleProducer<Integer> pa = new SingleProducer<Integer>(ts, 1);
        
        ts.unsubscribe();
        
        pa.request(1);
        
        ts.assertNoValues();
        ts.assertNoErrors();
        ts.assertNotCompleted();
    }

    @Test
    public void cancelAfterOnNext() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>();
        Observable.just(1).take(1).subscribe(ts);
        
        ts.assertValue(1);
        ts.assertNoErrors();
        ts.assertCompleted();
    }

    @Test
    public void onNextThrows() {
        TestSubscriber<Integer> ts = new TestSubscriber<Integer>(0) {
            @Override
            public void onNext(Integer t) {
                throw new TestException();
            }
        };
        SingleProducer<Integer> sp = new SingleProducer<Integer>(ts, 1);
        
        sp.request(1);
        
        ts.assertNoValues();
        ts.assertError(TestException.class);
        ts.assertNotCompleted();
    }

}
