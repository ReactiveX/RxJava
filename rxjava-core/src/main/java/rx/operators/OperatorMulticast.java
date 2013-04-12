package rx.operators;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

public class OperatorMulticast {
    public static <T> Func1<Observer<T>, Subscription> multicast(Observable<T> source, Func1<T, Boolean> predicate) {
        return null;
    }

}
