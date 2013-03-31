package rx.subjects;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.Test;

import org.mockito.Mockito;
import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.testing.UnsubscribeTester;
import rx.util.AtomicObservableSubscription;
import rx.util.SynchronizedObserver;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class PublishSubject<T> extends Subject<T, T> {
    public static <T> PublishSubject<T> create() {
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

        return new PublishSubject<T>(onSubscribe, observers);
    }

    private final ConcurrentHashMap<Subscription, Observer<T>> observers;

    protected PublishSubject(Func1<Observer<T>, Subscription> onSubscribe, ConcurrentHashMap<Subscription, Observer<T>> observers) {
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
            PublishSubject<Integer> publishSubject = PublishSubject.create();
            final AtomicReference<List<Notification<String>>> actualRef = new AtomicReference<List<Notification<String>>>();

            Observable<List<Notification<Integer>>> wNotificationsList = publishSubject.materialize().toList();
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
            }).subscribe(publishSubject);
            // the publishSubject has received an onComplete from the first subscribe because
            // it is synchronous and the next subscribe won't do anything.
            Observable.toObservable(-1, -2, -3).subscribe(publishSubject);

            List<Notification<Integer>> expected = new ArrayList<Notification<Integer>>();
            expected.add(new Notification<Integer>(-1));
            expected.add(new Notification<Integer>(-2));
            expected.add(new Notification<Integer>(-3));
            expected.add(new Notification<Integer>());
            Assert.assertTrue(actualRef.get().containsAll(expected));

            sub.unsubscribe();
        }

        private final Exception testException = new Exception();

        @Test
        public void testCompleted() {
            PublishSubject<Object> subject = PublishSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onCompleted();

            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            subject.onNext("four");
            subject.onCompleted();
            subject.onError(new Exception());

            assertCompletedObserver(aObserver);
// todo bug?            assertNeverObserver(anotherObserver);
        }

        private void assertCompletedObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        private void assertNeverObserver(Observer<String> aObserver)
        {
            verify(aObserver, Mockito.never()).onNext(any(String.class));
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, Mockito.never()).onCompleted();
        }

        @Test
        public void testError() {
            PublishSubject<Object> subject = PublishSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onError(testException);

            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            subject.onNext("four");
            subject.onError(new Exception());
            subject.onCompleted();

            assertErrorObserver(aObserver);
// todo bug?            assertNeverObserver(anotherObserver);
        }

        private void assertErrorObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, times(1)).onError(testException);
            verify(aObserver, Mockito.never()).onCompleted();
        }


        @Test
        public void testSubscribeMidSequence() {
            PublishSubject<Object> subject = PublishSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");

            assertObservedUntilTwo(aObserver);

            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            subject.onNext("three");
            subject.onCompleted();

            assertCompletedObserver(aObserver);
            assertCompletedStartingWithThreeObserver(anotherObserver);
        }


        private void assertCompletedStartingWithThreeObserver(Observer<String> aObserver)
        {
            verify(aObserver, Mockito.never()).onNext("one");
            verify(aObserver, Mockito.never()).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testUnsubscribeFirstObserver() {
            PublishSubject<Object> subject = PublishSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            Subscription subscription = subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");

            subscription.unsubscribe();
            assertObservedUntilTwo(aObserver);

            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            subject.onNext("three");
            subject.onCompleted();

            assertObservedUntilTwo(aObserver);
            assertCompletedStartingWithThreeObserver(anotherObserver);
        }

        private void assertObservedUntilTwo(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, Mockito.never()).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, Mockito.never()).onCompleted();
        }

        @Test
        public void testUnsubscribe()
        {
            UnsubscribeTester.test(new Func0<PublishSubject<Object>>()
                                   {
                                       @Override
                                       public PublishSubject<Object> call()
                                       {
                                           return PublishSubject.create();
                                       }
                                   }, new Action1<PublishSubject<Object>>()
                                   {
                                       @Override
                                       public void call(PublishSubject<Object> PublishSubject)
                                       {
                                           PublishSubject.onCompleted();
                                       }
                                   }, new Action1<PublishSubject<Object>>()
                                   {
                                       @Override
                                       public void call(PublishSubject<Object> PublishSubject)
                                       {
                                           PublishSubject.onError(new Exception());
                                       }
                                   }, new Action1<PublishSubject<Object>>()
                                   {
                                       @Override
                                       public void call(PublishSubject<Object> PublishSubject)
                                       {
                                           PublishSubject.onNext("one");
                                       }
                                   }
            );
        }
    }
}
