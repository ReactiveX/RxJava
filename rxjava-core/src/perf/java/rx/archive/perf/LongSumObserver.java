package rx.archive.perf;

import rx.Subscriber;

public class LongSumObserver extends Subscriber<Long> {

    public long sum = 0;

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        throw new RuntimeException(e);
    }

    @Override
    public void onNext(Long l) {
        sum += l;
    }
}