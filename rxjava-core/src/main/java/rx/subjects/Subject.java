package rx.subjects;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public abstract class Subject<I, O> extends Observable<O> implements Observer<I> {
    protected Subject()
    {
        super();
    }

    protected Subject(Func1<Observer<O>, Subscription> onSubscribe)
    {
        super(onSubscribe);
    }
}
