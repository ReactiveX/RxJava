package rx.observables;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public abstract class ConnectableObservable<T> extends Observable<T> {

    protected ConnectableObservable(Func1<Observer<T>, Subscription> onSubscribe) {
        super(onSubscribe);
    }

    public abstract Subscription connect();

}
