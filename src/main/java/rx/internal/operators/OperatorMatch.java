package rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

public final class OperatorMatch<T, R> implements Operator<R, T> {

    private final Func1<T, Func1<? super T, ? extends R>> findMappingFunc;

    public OperatorMatch(Func1<T, Func1<? super T, ? extends R>> findMappingFunc) {
        this.findMappingFunc = findMappingFunc;
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super R> o) {
        return new Subscriber<T>(o) {
            @Override
            public void onCompleted() {
                o.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(T t) {
                try {
                    Func1<? super T, ? extends R> mapping = findMappingFunc.call(t);
                    o.onNext(mapping.call(t));
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(OnErrorThrowable.addValueAsLastCause(e, t));
                }
            }
        };
    }
}