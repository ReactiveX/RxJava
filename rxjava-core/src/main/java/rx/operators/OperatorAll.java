package rx.operators;

import rx.Observable;
import rx.Subscriber;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

import java.util.concurrent.atomic.AtomicBoolean;

import static rx.Observable.Operator;

/**
 * Returns an Observable that emits a Boolean that indicates whether all items emitted by an
 * Observable satisfy a condition.
 * <p>
 * <img width="640" src="https://github.com/Netflix/RxJava/wiki/images/rx-operators/all.png">
 */
public class OperatorAll<T> implements Operator<Boolean,T>{

    private final Func1<? super T, Boolean> predicate;

    public OperatorAll(Func1<? super T, Boolean> predicate) {
        this.predicate = predicate;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super Boolean> child) {
        return new Subscriber<T>() {
            private AtomicBoolean status = new AtomicBoolean(true);

            @Override
            public void onCompleted() {
                child.onNext(status.get());
                child.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                child.onError(e);
            }

            @Override
            public void onNext(T t) {
                try {
                    final Boolean result = predicate.call(t);
                    boolean changed = status.compareAndSet(true, result);

                    if (changed && !result) {
                        child.onNext(false);
                        child.onCompleted();
                    }
                } catch (Throwable e) {
                    child.onError(OnErrorThrowable.addValueAsLastCause(e,t));
                }
            }
        };
    }
}
