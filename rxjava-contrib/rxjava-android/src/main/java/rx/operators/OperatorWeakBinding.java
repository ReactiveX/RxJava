package rx.operators;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.functions.Functions;
import rx.operators.OperatorWeakBinding.BoundPayload;

import android.util.Log;

import java.lang.ref.WeakReference;

/**
 * Ties a source sequence to the life-cycle of the given target object
 * using weak references. When the object is gone, this operator automatically unsubscribes
 * from the source sequence. The target object will be passed to the subscriber as well so
 * that no references (which would extend the object life) need to be kept within the subscriber.
 * <p/>
 * You can also pass in an optional predicate function, which whenever it evaluates to false
 * on the target object, will also result in the operator unsubscribing from the sequence.
 *
 * @param <T> the type of the objects emitted to a subscriber
 * @param <R> the type of the target object to bind to
 */
public final class OperatorWeakBinding<T, R> implements Observable.Operator<BoundPayload<R, T>, T> {

    private static final String LOG_TAG = "WeakBinding";

    public static final class BoundPayload<Target, Payload> {
        public final Target target;
        public final Payload payload;

        private BoundPayload(final Target target, final Payload payload) {
            if (target == null) {
                throw new IllegalArgumentException("target cannot be null");
            }
            this.target = target;
            this.payload = payload;
        }

        public static <Target, Payload> BoundPayload<Target, Payload> of(final Target target, final Payload payload) {
            return new BoundPayload<Target, Payload>(target, payload);
        }

        @Override
        public boolean equals(final Object other) {
            if (other instanceof BoundPayload<?, ?>) {
                final BoundPayload<?, ?> otherPayload = (BoundPayload<?, ?>) other;
                return otherPayload.target == target &&
                        (payload == null ?
                                otherPayload.payload == null :
                                payload.equals(otherPayload.payload));
            }
            return false;
        }

        @Override
        public int hashCode() {
            return target.hashCode() ^ (payload != null ? payload.hashCode() : 0);
        }

    }

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
    public Subscriber<? super T> call(final Subscriber<? super BoundPayload<R, T>> child) {
        return new ExtendedSubscriber(child);
    }

    final class ExtendedSubscriber extends Subscriber<T> {

        final Subscriber<? super BoundPayload<R, T>> subscriber;

        private ExtendedSubscriber(Subscriber<? super BoundPayload<R, T>> source) {
            super(source);
            subscriber = source;
        }

        @Override
        public void onCompleted() {
            if (shouldForwardNotification()) {
                subscriber.onCompleted();
            } else {
                handleLostBinding("onCompleted");
            }
        }

        @Override
        public void onError(Throwable e) {
            if (shouldForwardNotification()) {
                subscriber.onError(e);
            } else {
                handleLostBinding("onError");
            }
        }

        @Override
        public void onNext(T t) {
            final R target = boundRef.get();
            if (target != null && predicate.call(target)) {
                subscriber.onNext(BoundPayload.of(target, t));
            } else {
                handleLostBinding("onNext");
            }
        }

        private boolean shouldForwardNotification() {
            final R target = boundRef.get();
            return target != null && predicate.call(target);
        }

        private void handleLostBinding(String context) {
            if (boundRef.get() != null) {
                // the predicate failed to validate
                log("bound component has become invalid; skipping " + context);
            } else {
                log("bound component gone; skipping " + context);
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
