package rx.subjects;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public abstract class Subject<T, R> extends Observable<R> implements Observer<T> {
    protected Subject(Func1<Observer<R>, Subscription> onSubscribe) {
        super(onSubscribe);
    }
}
