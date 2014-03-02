package rx.operators;

import rx.Observable;
import rx.Subscriber;

import android.util.Log;

import java.lang.ref.WeakReference;

public final class OperatorWeakBinding<T, R> implements Observable.Operator<T, T> {

    private static final String LOG_TAG = "WeakBinding";

    private final WeakReference<R> boundRef;

    public OperatorWeakBinding(R bound) {
        boundRef = new WeakReference<R>(bound);
    }

    @Override
    public Subscriber<? super T> call(final Subscriber<? super T> child) {
        return new WeakSubscriber<T, R>(child, boundRef);
    }

    private static final class WeakSubscriber<T, R> extends Subscriber<T> {

        private final WeakReference<Subscriber<? super T>> subscriberRef;
        private final WeakReference<R> boundRef;

        private WeakSubscriber(Subscriber<? super T> op, WeakReference<R> boundRef) {
            subscriberRef = new WeakReference<Subscriber<? super T>>(op);
            this.boundRef = boundRef;
        }

        @Override
        public void onCompleted() {
            Subscriber<? super T> sub = subscriberRef.get();
            if (sub != null && boundRef.get() != null) {
                sub.onCompleted();
            } else {
                handleLostBinding(sub, "onCompleted");
            }
        }

        @Override
        public void onError(Throwable e) {
            Subscriber<? super T> sub = subscriberRef.get();
            if (sub != null && boundRef.get() != null) {
                sub.onError(e);
            } else {
                handleLostBinding(sub, "onError");
            }
        }

        @Override
        public void onNext(T t) {
            Subscriber<? super T> sub = subscriberRef.get();
            if (sub != null && boundRef.get() != null) {
                sub.onNext(t);
            } else {
                handleLostBinding(sub, "onNext");
            }
        }

        private void handleLostBinding(Subscriber<? super T> sub, String context) {
            if (sub == null) {
                Log.d(LOG_TAG, "subscriber gone; skipping " + context);
            } else {
                Log.d(LOG_TAG, "bound component gone; skipping " + context);
            }
            unsubscribe();
        }

    }
}
