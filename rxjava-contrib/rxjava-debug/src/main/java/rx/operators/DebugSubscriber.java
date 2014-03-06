package rx.operators;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.plugins.DebugNotification;
import rx.plugins.DebugNotificationListener;

public final class DebugSubscriber<T, C> extends Subscriber<T> {
    private DebugNotificationListener<C> listener;
    private final Observer<? super T> o;
    private Operator<? extends T, ?> from = null;
    private Operator<?, ? super T> to = null;

    public DebugSubscriber(
            DebugNotificationListener<C> listener,
            Subscriber<? super T> _o,
            Operator<? extends T, ?> _out,
            Operator<?, ? super T> _in) {
        super(_o);
        this.listener = listener;
        this.o = _o;
        this.from = _out;
        this.to = _in;
        this.add(new DebugSubscription<T, C>(this, listener));
    }

    @Override
    public void onCompleted() {
        final DebugNotification<T> n = DebugNotification.createOnCompleted(o, from, to);
        C context = listener.start(n);
        try {
            o.onCompleted();
            listener.complete(context);
        } catch (Throwable e) {
            listener.error(context, e);
        }
    }

    @Override
    public void onError(Throwable e) {
        final DebugNotification<T> n = DebugNotification.createOnError(o, from, e, to);
        C context = listener.start(n);
        try {
            o.onError(e);
            listener.complete(context);
        } catch (Throwable e2) {
            listener.error(context, e2);
        }
    }

    @Override
    public void onNext(T t) {
        final DebugNotification<T> n = DebugNotification.createOnNext(o, from, t, to);
        t = (T) listener.onNext(n);

        C context = listener.start(n);
        try {
            o.onNext(t);
            listener.complete(context);
        } catch (Throwable e) {
            listener.error(context, e);
        }
    }

    public Operator<? extends T, ?> getFrom() {
        return from;
    }

    public void setFrom(Operator<? extends T, ?> bind) {
        this.from = bind;
    }

    public Operator<?, ? super T> getTo() {
        return to;
    }

    public void setTo(Operator<?, ? super T> op) {
        this.to = op;
    }

    public Observer<? super T> getActual() {
        return o;
    }
}