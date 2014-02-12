package rx.plugins;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Action2;
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
public class DebugHook<C> extends RxJavaObservableExecutionHook {
    private final Func1 onNextHook;
    private final Func1<DebugNotification, C> start;
    private final Action1<C> complete;
    private final Action2<C, Throwable> error;

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
    public DebugHook(Func1 onNextDataHook, Func1<DebugNotification, C> start, Action1<C> complete, Action2<C, Throwable> error) {
        this.complete = complete;
        this.error = error;
        this.onNextHook = onNextDataHook == null ? Functions.identity() : onNextDataHook;
        this.start = (Func1<DebugNotification, C>) (start == null ? Actions.empty() : start);
    }

    @Override
    public <T> OnSubscribe<T> onSubscribeStart(final Observable<? extends T> observableInstance, final OnSubscribe<T> f) {
        return new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> o) {
                C context = start.call(DebugNotification.createSubscribe(o, observableInstance, f));
                try {
                    f.call(wrapOutbound(null, o));
                    complete.call(context);
                }
                catch(Throwable e) {
                    error.call(context, e);
                }
            }
        };
    }

    @Override
    public <T> Subscription onSubscribeReturn(Observable<? extends T> observableInstance, Subscription subscription) {
        return subscription;
    }

    @Override
    public <T> OnSubscribe<T> onCreate(final OnSubscribe<T> f) {
        return new OnCreateWrapper<T>(f);
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
                ((DebugSubscriber<R, C>) o).setFrom(bind);
            return o;
        }
        return new DebugSubscriber<R, C>(onNextHook, start, complete, error, o, bind, null);
    }

    @SuppressWarnings("unchecked")
    private <T> Subscriber<? super T> wrapInbound(Operator<?, ? super T> bind, Subscriber<? super T> o) {
        if (o instanceof DebugSubscriber) {
            if (bind != null)
                ((DebugSubscriber<T, C>) o).setTo(bind);
            return o;
        }
        return new DebugSubscriber<T, C>(onNextHook, start, complete, error, o, null, bind);
    }

    public final class OnCreateWrapper<T> implements OnSubscribe<T> {
        private final OnSubscribe<T> f;

        private OnCreateWrapper(OnSubscribe<T> f) {
            this.f = f;
        }

        @Override
        public void call(Subscriber<? super T> o) {
            f.call(wrapInbound(null, o));
        }

        public OnSubscribe<T> getActual() {
            return f;
        }
    }
}
