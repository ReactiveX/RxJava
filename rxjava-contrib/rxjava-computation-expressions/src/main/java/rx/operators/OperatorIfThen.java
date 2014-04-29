package rx.operators;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func0;

/**
 * Given a condition, subscribe to one of the observables when an Observer
 * subscribes.
 * 
 * @param <R>
 *            the result value type
 */
public final class OperatorIfThen<R> implements OnSubscribe<R> {
    final Func0<Boolean> condition;
    final Observable<? extends R> then;
    final Observable<? extends R> orElse;

    public OperatorIfThen(Func0<Boolean> condition, Observable<? extends R> then, Observable<? extends R> orElse) {
        this.condition = condition;
        this.then = then;
        this.orElse = orElse;
    }

    @Override
    public void call(Subscriber<? super R> t1) {
        Observable<? extends R> target;
        try {
            if (condition.call()) {
                target = then;
            } else {
                target = orElse;
            }
        } catch (Throwable t) {
            t1.onError(t);
            return;
        }
        target.subscribe(t1);
    }
}