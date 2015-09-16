package io.reactivex;

import java.util.concurrent.CountDownLatch;

import org.openjdk.jmh.infra.Blackhole;

public class LatchedObserver<T> extends Observer<T> {

    public CountDownLatch latch = new CountDownLatch(1);
    private final Blackhole bh;

    public LatchedObserver(Blackhole bh) {
        this.bh = bh;
    }

    @Override
    public void onComplete() {
        latch.countDown();
    }

    @Override
    public void onError(Throwable e) {
        latch.countDown();
    }

    @Override
    public void onNext(T t) {
        bh.consume(t);
    }

}