package rx.javafx.sources;

import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.scene.Node;
import rx.Observable;
import rx.Subscriber;
import rx.functions.Action0;
import rx.schedulers.JavaFxScheduler;
import rx.subscriptions.JavaFxSubscriptions;

public class NodeEventSource {
    /**
     * @see rx.observables.JavaFxObservable#fromNodeEvents
     */
    public static <T extends Event> Observable<T> fromNodeEvents(final Node source, final EventType<T> eventType) {

        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                final EventHandler<T> handler = new EventHandler<T>() {
                    @Override
                    public void handle(T t) {
                        subscriber.onNext(t);
                    }
                };

                source.addEventHandler(eventType, handler);

                subscriber.add(JavaFxSubscriptions.unsubscribeInEventDispatchThread(new Action0() {
                    @Override
                    public void call() {
                        source.removeEventHandler(eventType, handler);
                    }
                }));
            }

        }).subscribeOn(JavaFxScheduler.getInstance());
    }
}
