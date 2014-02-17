package rx.operators;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.plugins.DebugNotification;

public final class DebugSubscriber<T> extends Subscriber<T> {
    private final Func1<T, T> onNextHook;
    final Action1<DebugNotification> events;
    final Observer<? super T> o;
    Operator<? extends T, ?> from = null;
    Operator<?, ? super T> to = null;

    public DebugSubscriber(
            Func1<T, T> onNextHook,
            Action1<DebugNotification> _events,
            Subscriber<? super T> _o,
            Operator<? extends T, ?> _out,
            Operator<?, ? super T> _in) {
        super(_o);
        this.events = _events;
        this.o = _o;
        this.onNextHook = onNextHook;
        this.from = _out;
        this.to = _in;
        this.add(new DebugSubscription<T>(this));
    }

    @Override
    public void onCompleted() {
        events.call(DebugNotification.createOnCompleted(o, from, to));
        o.onCompleted();
    }

    @Override
    public void onError(Throwable e) {
        events.call(DebugNotification.createOnError(o, from, e, to));
        o.onError(e);
    }

    @Override
    public void onNext(T t) {
        events.call(DebugNotification.createOnNext(o, from, t, to));
        o.onNext(onNextHook.call(t));
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