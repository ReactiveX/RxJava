package rx.plugins;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observable.Operator;
import rx.Subscriber;
import rx.Subscription;
import rx.operators.DebugSubscriber;

/**
 * Implements hooks into the {@link Observable} chain to emit a detailed account of all the events
 * that happened.
 * 
 * @author gscampbell
 */
public class DebugHook<C> extends RxJavaObservableExecutionHook {
    private DebugNotificationListener<C> listener;

    /**
     * Creates a new instance of the DebugHook RxJava plug-in that can be passed into
     * {@link RxJavaPlugins} registerObservableExecutionHook(hook) method.
     * 
     * @param listener
     *            all of the onNext values are passed through this function to allow for
     *            manipulation of the values
     */
    public DebugHook(DebugNotificationListener<C> listener) {
        if (listener == null)
            throw new IllegalArgumentException("The debug listener must not be null");
        this.listener = listener;
    }

    @Override
    public <T> OnSubscribe<T> onSubscribeStart(final Observable<? extends T> observableInstance, final OnSubscribe<T> f) {
        return new OnSubscribe<T>() {
            @Override
            public void call(Subscriber<? super T> o) {
                final DebugNotification<T> n = DebugNotification.createSubscribe(o, observableInstance, f);
                o = wrapOutbound(null, o);

                C context = listener.start(n);
                try {
                    f.call(o);
                    listener.complete(context);
                }
                catch(Throwable e) {
                    listener.error(context, e);
                }
            }
        };
    }

    @Override
    public <T> Subscription onSubscribeReturn(Subscription subscription) {
        return subscription;
    }

    @Override
    public <T> OnSubscribe<T> onCreate(final OnSubscribe<T> f) {
        return new DebugOnSubscribe<T>(f);
    }
    
    public final class DebugOnSubscribe<T> implements OnSubscribe<T> {
        private final OnSubscribe<T> f;

        private DebugOnSubscribe(OnSubscribe<T> f) {
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


    @Override
    public <T, R> Operator<? extends R, ? super T> onLift(final Operator<? extends R, ? super T> bind) {
        return new Operator<R, T>() {
            @Override
            public Subscriber<? super T> call(final Subscriber<? super R> o) {
                return wrapInbound(bind, bind.call(wrapOutbound(bind, o)));
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <R> Subscriber<? super R> wrapOutbound(Operator<? extends R, ?> bind, Subscriber<? super R> o) {
        if (o instanceof DebugSubscriber) {
            if (bind != null)
                ((DebugSubscriber<R, C>) o).setFrom(bind);
            return o;
        }
        return new DebugSubscriber<R, C>(listener, o, bind, null);
    }

    @SuppressWarnings("unchecked")
    private <T> Subscriber<? super T> wrapInbound(Operator<?, ? super T> bind, Subscriber<? super T> o) {
        if (o instanceof DebugSubscriber) {
            if (bind != null)
                ((DebugSubscriber<T, C>) o).setTo(bind);
            return o;
        }
        return new DebugSubscriber<T, C>(listener, o, null, bind);
    }
}
