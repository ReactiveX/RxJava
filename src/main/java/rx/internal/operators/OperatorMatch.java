package rx.internal.operators;

import rx.Observable.Operator;
import rx.Subscriber;
import rx.exceptions.Exceptions;
import rx.exceptions.OnErrorThrowable;
import rx.functions.Func1;

import java.util.List;

public final class OperatorMatch<T, R> implements Operator<R, T> {

    public static class MatchPair<T,R> {
        final Func1<T, Boolean> predicate;
        final Func1<? super T, ? extends R> mapping;

        public MatchPair(Func1<T, Boolean> predicate, Func1<? super T, ? extends R> mapping) {
            this.predicate = predicate;
            this.mapping = mapping;
        }

        public Func1<T, Boolean> getPredicate() {
            return predicate;
        }

        public Func1<? super T, ? extends R> getMapping() {
            return mapping;
        }
    }

    private final List<MatchPair<T,R>> mappings;

    private final Func1<? super T, ? extends R> defaultMapping;

    public OperatorMatch(List<MatchPair<T,R>> mappings,
                         Func1<? super T, ? extends R> defaultMapping) {
        this.mappings = mappings;
        this.defaultMapping = defaultMapping;
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
                    int size = mappings.size();
                    for (int i = 0; i < size; i++) {
                        MatchPair<T,R> pair = mappings.get(i);
                        Func1<T, Boolean> predicate = pair.getPredicate();
                        if (predicate.call(t)) {
                            o.onNext(pair.getMapping().call(t));
                            return;
                        }
                    }
                    o.onNext(defaultMapping.call(t));
                } catch (Throwable e) {
                    Exceptions.throwIfFatal(e);
                    onError(OnErrorThrowable.addValueAsLastCause(e, t));
                }
            }
        };
    }
}