package rx.plugins;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Actions;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.operators.DebugSubscriber;

/**
 * Implements hooks into the {@link Observable} chain to emit a detailed account of all the events
 * that happened.
 * 
 * @author gscampbell
 */
public class DebugHook extends RxJavaObservableExecutionHook {
    private final Func1 onNextHook;
    private final Action1<DebugNotification> events;

    /**
     * Creates a new instance of the DebugHook RxJava plug-in that can be passed into
     * {@link RxJavaPlugins} registerObservableExecutionHook(hook) method.
     * 
     * @param onNextDataHook
     *            all of the onNext values are passed through this function to allow for
     *            manipulation of the values
     * @param events
     *            This action is invoked as each notification is generated
     */
    public DebugHook(Func1 onNextDataHook, Action1<DebugNotification> events) {
        this.onNextHook = onNextDataHook == null ? Functions.identity() : onNextDataHook;
        this.events = events == null ? Actions.empty() : events;
    }

    @Override
    public <T> OnSubscribe<T> onSubscribeStart(Observable<? extends T> observableInstance, final OnSubscribe<T> f) {
        return new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> o) {
                events.call(DebugNotification.createSubscribe(o, f));
                f.call(wrapOutbound(null, o));
            }
        };
    }

    @Override
    public <T> Subscription onSubscribeReturn(Observable<? extends T> observableInstance, Subscription subscription) {
        return subscription;
    }

    @Override
    public <T> OnSubscribe<T> onCreate(final OnSubscribe<T> f) {
        return new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> o) {
                f.call(wrapInbound(null, o));
            }
        };
    }

    @Override
    public <T, R> Operator<? extends R, ? super T> onLift(final Operator<? extends R, ? super T> bind) {
        return new Operator<R, T>() {
            @Override
            public Subscriber<? super T> call(final Subscriber<? super R> o) {
                return wrapInbound(bind, bind.call(wrapOutbound(bind, o)));
            }
        };
    }

    @Override
    public <T> Subscription onAdd(Subscriber<T> subscriber, Subscription s) {
        return s;
    }

    @SuppressWarnings("unchecked")
    private <R> Subscriber<? super R> wrapOutbound(Operator<? extends R, ?> bind, Subscriber<? super R> o) {
        if (o instanceof DebugSubscriber) {
            if (bind != null)
                ((DebugSubscriber<R>) o).setFrom(bind);
            return o;
        }
        return new DebugSubscriber<R>(onNextHook, events, o, bind, null);
    }

    @SuppressWarnings("unchecked")
    private <T> Subscriber<? super T> wrapInbound(Operator<?, ? super T> bind, Subscriber<? super T> o) {
        if (o instanceof DebugSubscriber) {
            if (bind != null)
                ((DebugSubscriber<T>) o).setTo(bind);
            return o;
        }
        return new DebugSubscriber<T>(onNextHook, events, o, null, bind);
    }
}
