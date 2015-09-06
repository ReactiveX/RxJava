package io.reactivex.internal.operators;

import static org.junit.Assert.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.LongConsumer;

import org.junit.Test;

import io.reactivex.Observable;
import io.reactivex.Observer;

public class OperatorDoOnRequestTest {

    @Test
    public void testUnsubscribeHappensAgainstParent() {
        final AtomicBoolean unsubscribed = new AtomicBoolean(false);
        Observable.just(1)
        //
                .doOnCancel(new Runnable() {
                    @Override
                    public void run() {
                        unsubscribed.set(true);
                    }
                })
                //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        // do nothing
                    }
                })
                //
                .subscribe();
        assertTrue(unsubscribed.get());
    }

    @Test
    public void testDoRequest() {
        final List<Long> requests = new ArrayList<>();
        Observable.range(1, 5)
        //
                .doOnRequest(new LongConsumer() {
                    @Override
                    public void accept(long n) {
                        requests.add(n);
                    }
                })
                //
                .subscribe(new Observer<Integer>() {

                    @Override
                    public void onStart() {
                        request(3);
                    }

                    @Override
                    public void onComplete() {

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