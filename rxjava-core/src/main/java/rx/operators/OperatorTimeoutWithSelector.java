package rx.operators;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.Exceptions;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperatorTimeoutWithSelector<T, U, V> extends
        OperatorTimeoutBase<T> {

    public OperatorTimeoutWithSelector(
            final Func0<? extends Observable<U>> firstTimeoutSelector,
            final Func1<? super T, ? extends Observable<V>> timeoutSelector,
            Observable<? extends T> other) {
        super(new FirstTimeoutStub<T>() {

            @Override
            public Subscription call(
                    final TimeoutSubscriber<T> timeoutSubscriber,
                    final Long seqId) {
                if (firstTimeoutSelector != null) {
                    Observable<U> o = null;
                    try {
                        o = firstTimeoutSelector.call();
                    } catch (Throwable t) {
                        Exceptions.throwIfFatal(t);
                        timeoutSubscriber.onError(t);
                        return Subscriptions.empty();
                    }
                    return o.subscribe(new Subscriber<U>() {

                        @Override
                        public void onCompleted() {
                            timeoutSubscriber.onTimeout(seqId);
                        }

                        @Override
                        public void onError(Throwable e) {
                            timeoutSubscriber.onError(e);
                        }

                        @Override
                        public void onNext(U t) {
                            timeoutSubscriber.onTimeout(seqId);
                        }

                    });
                } else {
                    return Subscriptions.empty();
                }
            }
        }, new TimeoutStub<T>() {

            @Override
            public Subscription call(
                    final TimeoutSubscriber<T> timeoutSubscriber,
                    final Long seqId, T value) {
                Observable<V> o = null;
                try {
                    o = timeoutSelector.call(value);
                } catch (Throwable t) {
                    Exceptions.throwIfFatal(t);
                    timeoutSubscriber.onError(t);
                    return Subscriptions.empty();
                }
                return o.subscribe(new Subscriber<V>() {

                    @Override
                    public void onCompleted() {
                        timeoutSubscriber.onTimeout(seqId);
                    }

                    @Override
                    public void onError(Throwable e) {
                        timeoutSubscriber.onError(e);
                    }

                    @Override
                    public void onNext(V t) {
                        timeoutSubscriber.onTimeout(seqId);
                    }

                });
            }
        }, other);
    }

}
