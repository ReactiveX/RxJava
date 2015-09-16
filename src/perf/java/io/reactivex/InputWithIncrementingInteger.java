package io.reactivex;

import java.util.Iterator;

import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.infra.Blackhole;
import org.reactivestreams.*;
import io.reactivex.internal.subscriptions.*;

/**
 * Exposes an Observable and Observer that increments n Integers and consumes them in a Blackhole.
 */
public abstract class InputWithIncrementingInteger {
    public Iterable<Integer> iterable;
    public Observable<Integer> observable;
    public Observable<Integer> firehose;
    public Blackhole bh;

    public abstract int getSize();

    @Setup
    public void setup(final Blackhole bh) {
        this.bh = bh;
        final int size = getSize();
        observable = Observable.range(0, size);

        firehose = Observable.create(new Publisher<Integer>() {

            @Override
            public void subscribe(Subscriber<? super Integer> s) {
                s.onSubscribe(EmptySubscription.INSTANCE);
                for (int i = 0; i < size; i++) {
                    s.onNext(i);
                }
                s.onComplete();
            }

        });
        iterable = new Iterable<Integer>() {
            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {
                    int i = 0;
                    
                    @Override
                    public boolean hasNext() {
                        return i < size;
                    }
                    
                    @Override
                    public Integer next() {
                        Blackhole.consumeCPU(10);
                        return i++;
                    }
                    
                    @Override
                    public void remove() {
                        
                    }
                };
            }
        };

    }

    public LatchedObserver<Integer> newLatchedObserver() {
        return new LatchedObserver<>(bh);
    }

    public Subscriber<Integer> newSubscriber() {
        return new Observer<Integer>() {

            @Override
            public void onComplete() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Integer t) {
                bh.consume(t);
            }

        };
    }

}