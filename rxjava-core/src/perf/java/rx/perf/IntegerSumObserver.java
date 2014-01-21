package rx.perf;

import rx.Observer;

public class IntegerSumObserver implements Observer<Integer> {

    public int sum = 0;

    @Override
    public void onCompleted() {

    }

    @Override
    public void onError(Throwable e) {
        throw new RuntimeException(e);
    }

    @Override
    public void onNext(Integer l) {
        sum += l;
    }
}