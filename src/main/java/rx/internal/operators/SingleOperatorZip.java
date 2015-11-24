package rx.internal.operators;

import rx.Single;
import rx.SingleSubscriber;
import rx.exceptions.Exceptions;
import rx.functions.FuncN;
import rx.plugins.RxJavaPlugins;
import rx.subscriptions.CompositeSubscription;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class SingleOperatorZip {

    public static <T, R> Single<R> zip(final Single<? extends T>[] singles, final FuncN<? extends R> zipper) {
        return Single.create(new Single.OnSubscribe<R>() {
            @Override
            public void call(final SingleSubscriber<? super R> subscriber) {
                final AtomicInteger wip = new AtomicInteger(singles.length);
                final AtomicBoolean once = new AtomicBoolean();
                final Object[] values = new Object[singles.length];

                CompositeSubscription compositeSubscription = new CompositeSubscription();
                subscriber.add(compositeSubscription);

                for (int i = 0; i < singles.length; i++) {
                    if (compositeSubscription.isUnsubscribed() || once.get()) {
                        break;
                    }

                    final int j = i;
                    SingleSubscriber<T> singleSubscriber = new SingleSubscriber<T>() {
                        @Override
                        public void onSuccess(T value) {
                            values[j] = value;
                            if (wip.decrementAndGet() == 0) {
                                R r;

                                try {
                                    r = zipper.call(values);
                                } catch (Throwable e) {
                                    Exceptions.throwIfFatal(e);
                                    onError(e);
                                    return;
                                }

                                subscriber.onSuccess(r);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            if (once.compareAndSet(false, true)) {
                                subscriber.onError(error);
                            } else {
                                RxJavaPlugins.getInstance().getErrorHandler().handleError(error);
                            }
                        }
                    };

                    compositeSubscription.add(singleSubscriber);

                    if (compositeSubscription.isUnsubscribed() || once.get()) {
                        break;
                    }

                    singles[i].subscribe(singleSubscriber);
                }
            }
        });
    }
}
