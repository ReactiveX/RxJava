package rx.operators;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Functions;

import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Ties a source sequence to the life-cycle of the given target object, and/or the subscriber
 * using weak references. When either object is gone, this operator automatically unsubscribes
 * from the source sequence.
 * <p/>
 * You can also pass in an optional predicate function, which whenever it evaluates to false
 * on the target object, will also result in the operator unsubscribing from the sequence.
 *
 * @param <T> the type of the objects emitted to a subscriber
 * @param <R> the type of the target object to bind to
 */
public final class OperatorWeakBinding<T, R> implements Observable.Operator<T, T> {

    private static final String LOG_TAG = "WeakBinding";

    final WeakReference<R> boundRef;
    private final Func1<? super R, Boolean> predicate;

    public OperatorWeakBinding(R bound, Func1<? super R, Boolean> predicate) {
        boundRef = new WeakReference<R>(bound);
        this.predicate = predicate;
    }

    public OperatorWeakBinding(R bound) {
        boundRef = new WeakReference<R>(bound);
        this.predicate = Functions.alwaysTrue();
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new WeakSubscriber(child);
    }

    final class WeakSubscriber extends Subscriber<T> {

        final WeakReference<Subscriber<? super T>> subscriberRef;

        private WeakSubscriber(Subscriber<? super T> source) {
            super(source);
            subscriberRef = new WeakReference<Subscriber<? super T>>(source);
        }

        @Override
        public void onCompleted() {
            final Subscriber<? super T> sub = subscriberRef.get();
            if (shouldForwardNotification(sub)) {
                sub.onCompleted();
            } else {
                handleLostBinding(sub, "onCompleted");
            }
        }

        @Override
        public void onError(Throwable e) {
            final Subscriber<? super T> sub = subscriberRef.get();
            if (shouldForwardNotification(sub)) {
                sub.onError(e);
            } else {
                handleLostBinding(sub, "onError");
            }
        }

        @Override
        public void onNext(T t) {
            final Subscriber<? super T> sub = subscriberRef.get();
            if (shouldForwardNotification(sub)) {
                sub.onNext(t);
            } else {
                handleLostBinding(sub, "onNext");
            }
        }

        private boolean shouldForwardNotification(Subscriber<? super T> sub) {
            final R target = boundRef.get();
            return sub != null && target != null && predicate.call(target);
        }

        private void handleLostBinding(Subscriber<? super T> sub, String context) {
            if (sub == null) {
                log("subscriber gone; skipping " + context);
            } else {
                final R r = boundRef.get();
                if (r != null) {
                    // the predicate failed to validate
                    log("bound component has become invalid; skipping " + context);
                } else {
                    log("bound component gone; skipping " + context);
                }
            }
            log("unsubscribing...");
            unsubscribe();
        }

        private void log(String message) {
            if (Log.isLoggable(LOG_TAG, Log.DEBUG)) {
                Log.d(LOG_TAG, message);
            }
        }
    }
}
