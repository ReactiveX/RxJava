package rx;

import rx.subscriptions.CompositeSubscription;

public abstract class Operator<T> implements Observer<T>, Subscription {

    private final CompositeSubscription cs;

    public Operator() {
        this.cs = new CompositeSubscription();
    }
    
    // TODO I'm questioning this API, it could be confusing and misused
    protected Operator(Operator<?> op) {
        this.cs = op.cs;
    }

    protected Operator(CompositeSubscription cs) {
        this.cs = cs;
    }

    public static <T> Operator<T> create(final Observer<? super T> o, CompositeSubscription cs) {
        if (o == null) {
            throw new IllegalArgumentException("Observer can not be null");
        }
        if (cs == null) {
            throw new IllegalArgumentException("CompositeSubscription can not be null");
        }
        return new Operator<T>(cs) {

            @Override
            public void onCompleted() {
                o.onCompleted();
            }

            @Override
            public void onError(Throwable e) {
                o.onError(e);
            }

            @Override
            public void onNext(T v) {
                o.onNext(v);
            }

        };
    }

    public static <T> Operator<T> create(final Observer<? super T> o, Subscription s) {
        if (s == null) {
            throw new IllegalArgumentException("Subscription can not be null");
        }
        CompositeSubscription cs = new CompositeSubscription();
        cs.add(s);

        return create(o, cs);
    }

    /**
     * Used to register an unsubscribe callback.
     */
    public final void add(Subscription s) {
        cs.add(s);
    }

    @Override
    public final void unsubscribe() {
        cs.unsubscribe();
    }

    public final boolean isUnsubscribed() {
        return cs.isUnsubscribed();
    }
}