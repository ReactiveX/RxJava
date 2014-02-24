package rx.operators;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Action2;
import rx.functions.Func1;
import rx.plugins.DebugNotification;

public final class DebugSubscriber<T, C> extends Subscriber<T> {
    private final Func1<T, T> onNextHook;
    private final Func1<DebugNotification, C> start;
    private final Action1<C> complete;
    private final Action2<C, Throwable> error;
    private final Observer<? super T> o;
    private Operator<? extends T, ?> from = null;
    private Operator<?, ? super T> to = null;

    public DebugSubscriber(
            Func1<T, T> onNextHook,
            Func1<DebugNotification, C> start,
            Action1<C> complete,
            Action2<C, Throwable> error,
            Subscriber<? super T> _o,
            Operator<? extends T, ?> _out,
            Operator<?, ? super T> _in) {
        super(_o);
        this.start = start;
        this.complete = complete;
        this.error = error;
        this.o = _o;
        this.onNextHook = onNextHook;
        this.from = _out;
        this.to = _in;
        this.add(new DebugSubscription<T, C>(this, start, complete, error));
    }

    @Override
    public void onCompleted() {
        final DebugNotification<T, C> n = DebugNotification.createOnCompleted(o, from, to);
        C context = start.call(n);
        try {
            o.onCompleted();
            complete.call(context);
        } catch (Throwable e) {
            error.call(context, e);
        }
    }

    @Override
    public void onError(Throwable e) {
        final DebugNotification<T, C> n = DebugNotification.createOnError(o, from, e, to);
        C context = start.call(n);
        try {
            o.onError(e);
            complete.call(context);
        } catch (Throwable e2) {
            error.call(context, e2);
        }
    }

    @Override
    public void onNext(T t) {
        final DebugNotification<T, C> n = DebugNotification.createOnNext(o, from, t, to);
        C context = start.call(n);
        try {
            o.onNext(onNextHook.call(t));
            complete.call(context);
        } catch (Throwable e) {
            error.call(context, e);
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