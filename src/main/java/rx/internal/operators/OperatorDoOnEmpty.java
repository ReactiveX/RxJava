package rx.internal.operators;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.functions.Action0;

public final class OperatorDoOnEmpty<T> implements Observable.Operator<T, T> {

    private final Action0 onEmpty;

    public OperatorDoOnEmpty(Action0 onEmpty) {
        this.onEmpty = onEmpty;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {

        return new Subscriber<T>(child) {

            private boolean isEmpty = true;
            private boolean done = false;

            @Override
            public void onCompleted() {
                if (done) {
                    return;
                }
                if (isEmpty) {
                    try {
                        onEmpty.call();
                    } catch (Throwable e) {
                        Exceptions.throwOrReport(e,this);
                        return;
                    }
                    if (!isUnsubscribed()) {
                        child.onCompleted();
                    }
                } else {
                    child.onCompleted();
                }
                done = true;
            }

            @Override
            public void onError(Throwable e) {
                if (done) {
                    return;
                }
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                if (done) {
                    return;
                }
                isEmpty = false;
                child.onNext(t);
            }
        };
    }

}
