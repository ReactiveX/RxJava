package rx.javafx.sources;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.subscriptions.JavaFxSubscriptions;

public class ObservableValueSource {

    /**
     * @see rx.observables.JavaFxObservable#fromObservableValue
     */
    public static <T> Observable<T> fromObservableValue(final ObservableValue<T> fxObservable) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                subscriber.onNext(fxObservable.getValue());

                final ChangeListener<T> listener = new ChangeListener<T>() {
                    @Override
                    public void changed(final ObservableValue<? extends T> observableValue, final T prev, final T current) {
                        subscriber.onNext(current);
                    }
                };

                fxObservable.addListener(listener);

                subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(new Action0() {
                    @Override
                    public void call() {
                        fxObservable.removeListener(listener);
                    }
                }));

            }
        });
    }


}
