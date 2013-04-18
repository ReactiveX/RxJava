package rx.subjects;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.Subject;
import rx.util.AtomicObservableSubscription;

/**
 * An {@link Observable} that behaves like a {@link Subject} and can be <code>connect</code>ed (subscribed) to a source {@link Observable}.
 *
 * @param <T>
 */

public class ConnectableObservable<T> extends Observable<T> {

    private Subject<T> subject;
    private Observable<T> source;
    private Subscription subscription;
    private Object lock = new Object();

    /**
     * Constructs a {@link ConnectableObservable} that subscribes <code>subject</code> to <code>source</code> when {@link #connect()} is called.
     *
     * @param source
     *            {@link Observable} to connect the <code>subject</code> to.
     * @param subject
     *            The {@link Subject} that will provide the {@link Observable} behaviour.
     */
    public ConnectableObservable(Observable<T> source, Subject<T> subject) {
        this.subject = subject;
        this.source = source;
    }

    /**
     * Constructs a {@link ConnectableObservable} that subscribes to <code>source</code> when {@link #connect()} is called.
     * @param source
     *            {@link Observable} to connect the <code>subject</code> to.
     */
    public ConnectableObservable(Observable<T> source) {
        this.subject = Subject.create();
        this.source = source;
    }

    /**
     * Subscribes <code>observer</code> to the {@link Subject}.
     */
    public Subscription subscribe(Observer<T> observer) {
        return subject.subscribe(observer);
    }

    /**
     * Subscribes the {@link Subject} to the source {@link Observable}, if it was not subscribed already.
     * <p>
     * If it was subscribed still, it returns the existing {@link Subscription}.
     * @return a {@link Subscription} that can be used to unsubscribe from the source again.
     */
    public Subscription connect() {
        synchronized(lock) {
            if (subscription == null) {
                final Subscription actualSubscription = source.subscribe(subject);
                this.subscription = new AtomicObservableSubscription(
                    new Subscription() {
                        public void unsubscribe() {
                            synchronized(lock) {
                                actualSubscription.unsubscribe();
                                subscription = null;
                            }
                        }
                    }
                );
            }
            return subscription;
        }
    }
}
