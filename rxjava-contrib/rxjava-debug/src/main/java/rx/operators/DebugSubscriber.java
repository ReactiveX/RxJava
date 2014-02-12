package rx.operators;

import rx.Observable.Operator;
import rx.Observer;
import rx.Subscriber;
import rx.plugins.DebugNotification;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public final class DebugSubscriber<T> extends Subscriber<T> {
    private final Func1<T, T> onNextHook;
    final Action1<DebugNotification> events;
    final Observer<? super T> o;
    Operator<T, ?> from = null;
    Operator<?, T> to = null;

    public DebugSubscriber(
            Func1<T, T> onNextHook,
            Action1<DebugNotification> _events,
            Subscriber<? super T> _o,
            Operator<T, ?> _out,
            Operator<?, T> _in) {
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

    public Operator<T, ?> getFrom() {
        return from;
    }

    public void setFrom(Operator<T, ?> op) {
        this.from = op;
    }

    public Operator<?, T> getTo() {
        return to;
    }

    public void setTo(Operator<?, T> op) {
        this.to = op;
    }

    public Observer<? super T> getActual() {
        return o;
    }
}