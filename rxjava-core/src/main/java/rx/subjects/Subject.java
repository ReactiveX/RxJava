package rx.subjects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.Test;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.AtomicObservableSubscription;
import rx.util.SynchronizedObserver;
import rx.util.functions.Action1;
import rx.util.functions.Func1;

public class Subject<T> extends Observable<T> implements Observer<T> {
    public static <T> Subject<T> create() {
        final ConcurrentHashMap<Subscription, Observer<T>> observers = new ConcurrentHashMap<Subscription, Observer<T>>();

        Func1<Observer<T>, Subscription> onSubscribe = new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(Observer<T> observer) {
                final AtomicObservableSubscription subscription = new AtomicObservableSubscription();

                subscription.wrap(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        // on unsubscribe remove it from the map of outbound observers to notify
                        observers.remove(subscription);
                    }
                });

                // on subscribe add it to the map of outbound observers to notify
                observers.put(subscription, new SynchronizedObserver<T>(observer, subscription));
                return subscription;
            }
        };

        return new Subject<T>(onSubscribe, observers);
    }

    private final ConcurrentHashMap<Subscription, Observer<T>> observers;

    protected Subject(Func1<Observer<T>, Subscription> onSubscribe, ConcurrentHashMap<Subscription, Observer<T>> observers) {
        super(onSubscribe);
        this.observers = observers;
    }

    @Override
    public void onCompleted() {
        for (Observer<T> observer : observers.values()) {
            observer.onCompleted();
        }
    }

    @Override
    public void onError(Exception e) {
        for (Observer<T> observer : observers.values()) {
            observer.onError(e);
        }
    }

    @Override
    public void onNext(T args) {
        for (Observer<T> observer : observers.values()) {
            observer.onNext(args);
        }
    }

    public static class UnitTest {
        @Test
        public void test() {
            Subject<Integer> subject = Subject.create();
            final AtomicReference<List<Notification<String>>> actualRef = new AtomicReference<List<Notification<String>>>();

            Observable<List<Notification<Integer>>> wNotificationsList = subject.materialize().toList();
            wNotificationsList.subscribe(new Action1<List<Notification<String>>>() {
                @Override
                public void call(List<Notification<String>> actual) {
                    actualRef.set(actual);
                }
            });

            Subscription sub = Observable.create(new Func1<Observer<Integer>, Subscription>() {
                @Override
                public Subscription call(final Observer<Integer> observer) {
                    final AtomicBoolean stop = new AtomicBoolean(false);
                    new Thread() {
                        @Override
                        public void run() {
                            int i = 1;
                            while (!stop.get()) {
                                observer.onNext(i++);
                            }
                            observer.onCompleted();
                        }
                    }.start();
                    return new Subscription() {
                        @Override
                        public void unsubscribe() {
                            stop.set(true);
                        }
                    };
                }
            }).subscribe(subject);
            // the subject has received an onComplete from the first subscribe because
            // it is synchronous and the next subscribe won't do anything.
            Observable.toObservable(-1, -2, -3).subscribe(subject);

            List<Notification<Integer>> expected = new ArrayList<Notification<Integer>>();
            expected.add(new Notification<Integer>(-1));
            expected.add(new Notification<Integer>(-2));
            expected.add(new Notification<Integer>(-3));
            expected.add(new Notification<Integer>());
            Assert.assertTrue(actualRef.get().containsAll(expected));

            sub.unsubscribe();
        }
    }
}
