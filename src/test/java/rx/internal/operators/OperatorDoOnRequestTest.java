package rx.internal.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.functions.Action1;

public class OperatorDoOnRequestTest {

    @Test
    public void testUnsubscribeHappensAgainstParent() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Observable.just(1)
        //
                .doOnUnsubscribe(new Action0() {
                    @Override
                    public void call() {
                        unsubscribed.set(true);
                    }
                })
                //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        // do nothing
                    }
                })
                //
                .subscribe();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testDoRequest() {
        final List<Long> requests = new ArrayList<Long>();
        Observable.range(1, 5)
        //
                .doOnRequest(new Action1<Long>() {
                    @Override
                    public void call(Long n) {
                        requests.add(n);
                    }
                })
                //
                .subscribe(new Subscriber<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onNext(Integer t) {
                        request(t);
                    }
                });
        assertEquals(Arrays.asList(3L,1L,2L,3L,4L,5L), requests);
    }

}
