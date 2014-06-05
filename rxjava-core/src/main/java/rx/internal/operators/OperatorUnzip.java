package rx.internal.operators;

import java.util.concurrent.atomic.AtomicInteger;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.FuncN;
import rx.observables.ConnectableObservable;
import rx.observers.SafeSubscriber;

public class OperatorUnzip<T, R> {
    public static <T, R> R unzip(Observable<T> source, FuncN<R> unzipFunc, final Func1<T, ?>... selectFuncs) {
        final Object[] observables = new Observable[selectFuncs.length];

        final ConnectableObservable<T> connectable = source.publish();

        final AtomicInteger subscribeCount = new AtomicInteger(0);

        for (int i = 0; i < selectFuncs.length; i++) {
            final Func1<T, ?> selectFunc = selectFuncs[i];
            observables[i] = Observable.create(new OnSubscribe<Object>() {
                @Override
                public void call(Subscriber<? super Object> child) {
                    final SafeSubscriber<Object> s = new SafeSubscriber<Object>(child);

                    connectable.map(selectFunc).unsafeSubscribe(s);

                    if (subscribeCount.incrementAndGet() == selectFuncs.length) {
                        connectable.connect(new Action1<Subscription>() {
                            @Override
                            public void call(Subscription t1) {
                                s.add(t1);
                            }
                        });
                    }
                }
            });
        }

        return unzipFunc.call(observables);
    }
}
