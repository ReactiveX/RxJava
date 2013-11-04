package rx.operators;

import rx.Observable;
import rx.Observable.OnSubscribeFunc;
import rx.util.functions.Func1;

/**
 * Converts the elements of an observable sequence to the specified type.
 */
public class OperationCast {

    public static <T, R> OnSubscribeFunc<R> cast(
            Observable<? extends T> source, final Class<R> klass) {
        return OperationMap.map(source, new Func1<T, R>() {
            public R call(T t) {
                return klass.cast(t);
            }
        });
    }
}
