package rx.perf;

import rx.Observer;

public class LongSumObserver extends Observer<Long> {

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