/**
 * Copyright 2013 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.subjects;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

import rx.Notification;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.operators.SafeObservableSubscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action1;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

/**
 * Subject that, once and {@link Observer} has subscribed, publishes all subsequent events to the subscriber.
 *
 * <p>
 * Example usage:
 * <p>
 * <pre> {@code

  PublishSubject<Object> subject = PublishSubject.create();
  // observer1 will receive all onNext and onCompleted events
  subject.subscribe(observer1);
  subject.onNext("one");
  subject.onNext("two");
  // observer2 will only receive "three" and onCompleted
  subject.subscribe(observer2);
  subject.onNext("three");
  subject.onCompleted();

  } </pre>
 *
 * @param <T>
 */
public class PublishSubject<T> extends Subject<T, T> {
    public static <T> PublishSubject<T> create() {
        final ConcurrentHashMap<Subscription, Observer<T>> observers = new ConcurrentHashMap<Subscription, Observer<T>>();
        final AtomicReference<Notification<T>> terminalState = new AtomicReference<Notification<T>>();

        Func1<Observer<T>, Subscription> onSubscribe = new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(Observer<T> observer) {
                // shortcut check if terminal state exists already
                Subscription s = checkTerminalState(observer);
                if(s != null) return s;

                final SafeObservableSubscription subscription = new SafeObservableSubscription();

                subscription.wrap(new Subscription() {
                    @Override
                    public void unsubscribe() {
                        // on unsubscribe remove it from the map of outbound observers to notify
                        observers.remove(subscription);
                    }
                });

                /**
                 * NOTE: We are synchronizing to avoid a race condition between terminalState being set and
                 * a new observer being added to observers.
                 *
                 * The synchronization only occurs on subscription and terminal states, it does not affect onNext calls
                 * so a high-volume hot-observable will not pay this cost for emitting data.
                 *
                 * Due to the restricted impact of blocking synchronization here I have not pursued more complicated
                 * approaches to try and stay completely non-blocking.
                 */
                synchronized (terminalState) {
                    // check terminal state again
                    s = checkTerminalState(observer);
                    if (s != null)
                        return s;

                    // on subscribe add it to the map of outbound observers to notify
                    observers.put(subscription, observer);

                    return subscription;
                }
            }

            private Subscription checkTerminalState(Observer<T> observer) {
                Notification<T> n = terminalState.get();
                if (n != null) {
                    // we are terminated to immediately emit and don't continue with subscription
                    if (n.isOnCompleted()) {
                        observer.onCompleted();
                    } else {
                        observer.onError(n.getThrowable());
                    }
                    return Subscriptions.empty();
                } else {
                    return null;
                }
            }
        };

        return new PublishSubject<T>(onSubscribe, observers, terminalState);
    }

    private final ConcurrentHashMap<Subscription, Observer<T>> observers;
    private final AtomicReference<Notification<T>> terminalState;

    protected PublishSubject(Func1<Observer<T>, Subscription> onSubscribe, ConcurrentHashMap<Subscription, Observer<T>> observers, AtomicReference<Notification<T>> terminalState) {
        super(onSubscribe);
        this.observers = observers;
        this.terminalState = terminalState;
    }

    @Override
    public void onCompleted() {
        /**
         * Synchronizing despite terminalState being an AtomicReference because of multi-step logic in subscription.
         * Why use AtomicReference then? Convenient for passing around a mutable reference holder between the
         * onSubscribe function and PublishSubject instance... and it's a "better volatile" for the shortcut codepath.
         */
        synchronized (terminalState) {
            terminalState.set(new Notification<T>());
        }
        for (Observer<T> observer : snapshotOfValues()) {
            observer.onCompleted();
        }
        observers.clear();
    }

    @Override
    public void onError(Throwable e) {
        /**
         * Synchronizing despite terminalState being an AtomicReference because of multi-step logic in subscription.
         * Why use AtomicReference then? Convenient for passing around a mutable reference holder between the
         * onSubscribe function and PublishSubject instance... and it's a "better volatile" for the shortcut codepath.
         */
        synchronized (terminalState) {
            terminalState.set(new Notification<T>(e));
        }
        for (Observer<T> observer : snapshotOfValues()) {
            observer.onError(e);
        }
        observers.clear();
    }

    @Override
    public void onNext(T args) {
        for (Observer<T> observer : snapshotOfValues()) {
            observer.onNext(args);
        }
    }

    /**
     * Current snapshot of 'values()' so that concurrent modifications aren't included.
     *
     * This makes it behave deterministically in a single-threaded execution when nesting subscribes.
     *
     * In multi-threaded execution it will cause new subscriptions to wait until the following onNext instead
     * of possibly being included in the current onNext iteration.
     *
     * @return List<Observer<T>>
     */
    private Collection<Observer<T>> snapshotOfValues() {
        return new ArrayList<Observer<T>>(observers.values());
    }

    public static class UnitTest {
        @Test
        public void test() {
            PublishSubject<Integer> subject = PublishSubject.create();
            final AtomicReference<List<Notification<Integer>>> actualRef = new AtomicReference<List<Notification<Integer>>>();

            Observable<List<Notification<Integer>>> wNotificationsList = subject.materialize().toList();
            wNotificationsList.subscribe(new Action1<List<Notification<Integer>>>() {
                @Override
                public void call(List<Notification<Integer>> actual) {
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
            Observable.from(-1, -2, -3).subscribe(subject);

            List<Notification<Integer>> expected = new ArrayList<Notification<Integer>>();
            expected.add(new Notification<Integer>(-1));
            expected.add(new Notification<Integer>(-2));
            expected.add(new Notification<Integer>(-3));
            expected.add(new Notification<Integer>());
            Assert.assertTrue(actualRef.get().containsAll(expected));

            sub.unsubscribe();
        }

        private final Throwable testException = new Throwable();

        @Test
        public void testCompleted() {
            PublishSubject<String> subject = PublishSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onCompleted();

            @SuppressWarnings("unchecked")
            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            subject.onNext("four");
            subject.onCompleted();
            subject.onError(new Throwable());

            assertCompletedObserver(aObserver);
            // todo bug?            assertNeverObserver(anotherObserver);
        }

        private void assertCompletedObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testError() {
            PublishSubject<String> subject = PublishSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onError(testException);

            @SuppressWarnings("unchecked")
            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            subject.onNext("four");
            subject.onError(new Throwable());
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
            PublishSubject<String> subject = PublishSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");

            assertObservedUntilTwo(aObserver);

            @SuppressWarnings("unchecked")
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
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testUnsubscribeFirstObserver() {
            PublishSubject<String> subject = PublishSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> aObserver = mock(Observer.class);
            Subscription subscription = subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");

            subscription.unsubscribe();
            assertObservedUntilTwo(aObserver);

            @SuppressWarnings("unchecked")
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
            verify(aObserver, Mockito.never()).onError(any(Throwable.class));
            verify(aObserver, Mockito.never()).onCompleted();
        }

        /**
         * Test that subscribing after onError/onCompleted immediately terminates instead of causing it to hang.
         *
         * Nothing is mentioned in Rx Guidelines for what to do in this case so I'm doing what seems to make sense
         * which is:
         *
         * - cache terminal state (onError/onCompleted)
         * - any subsequent subscriptions will immediately receive the terminal state rather than start a new subscription
         *
         */
        @Test
        public void testUnsubscribeAfterOnCompleted() {
            PublishSubject<String> subject = PublishSubject.create();

            @SuppressWarnings("unchecked")
            Observer<String> anObserver = mock(Observer.class);
            subject.subscribe(anObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onCompleted();

            InOrder inOrder = inOrder(anObserver);
            inOrder.verify(anObserver, times(1)).onNext("one");
            inOrder.verify(anObserver, times(1)).onNext("two");
            inOrder.verify(anObserver, times(1)).onCompleted();
            inOrder.verify(anObserver, Mockito.never()).onError(any(Throwable.class));

            @SuppressWarnings("unchecked")
            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            inOrder = inOrder(anotherObserver);
            inOrder.verify(anotherObserver, Mockito.never()).onNext("one");
            inOrder.verify(anotherObserver, Mockito.never()).onNext("two");
            inOrder.verify(anotherObserver, times(1)).onCompleted();
            inOrder.verify(anotherObserver, Mockito.never()).onError(any(Throwable.class));
        }

        @Test
        public void testUnsubscribeAfterOnError() {
            PublishSubject<String> subject = PublishSubject.create();
            RuntimeException exception = new RuntimeException("failure");

            @SuppressWarnings("unchecked")
            Observer<String> anObserver = mock(Observer.class);
            subject.subscribe(anObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onError(exception);

            InOrder inOrder = inOrder(anObserver);
            inOrder.verify(anObserver, times(1)).onNext("one");
            inOrder.verify(anObserver, times(1)).onNext("two");
            inOrder.verify(anObserver, times(1)).onError(exception);
            inOrder.verify(anObserver, Mockito.never()).onCompleted();

            @SuppressWarnings("unchecked")
            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);

            inOrder = inOrder(anotherObserver);
            inOrder.verify(anotherObserver, Mockito.never()).onNext("one");
            inOrder.verify(anotherObserver, Mockito.never()).onNext("two");
            inOrder.verify(anotherObserver, times(1)).onError(exception);
            inOrder.verify(anotherObserver, Mockito.never()).onCompleted();
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
                public void call(PublishSubject<Object> DefaultSubject)
                {
                    DefaultSubject.onCompleted();
                }
            }, new Action1<PublishSubject<Object>>()
            {
                @Override
                public void call(PublishSubject<Object> DefaultSubject)
                {
                    DefaultSubject.onError(new Throwable());
                }
            }, new Action1<PublishSubject<Object>>()
            {
                @Override
                public void call(PublishSubject<Object> DefaultSubject)
                {
                    DefaultSubject.onNext("one");
                }
            });
        }

        @Test
        public void testNestedSubscribe() {
            final PublishSubject<Integer> s = PublishSubject.create();

            final AtomicInteger countParent = new AtomicInteger();
            final AtomicInteger countChildren = new AtomicInteger();
            final AtomicInteger countTotal = new AtomicInteger();

            final ArrayList<String> list = new ArrayList<String>();

            s.mapMany(new Func1<Integer, Observable<String>>() {

                @Override
                public Observable<String> call(final Integer v) {
                    countParent.incrementAndGet();

                    // then subscribe to subject again (it will not receive the previous value)
                    return s.map(new Func1<Integer, String>() {

                        @Override
                        public String call(Integer v2) {
                            countChildren.incrementAndGet();
                            return "Parent: " + v + " Child: " + v2;
                        }

                    });
                }

            }).subscribe(new Action1<String>() {

                @Override
                public void call(String v) {
                    countTotal.incrementAndGet();
                    list.add(v);
                }

            });


            for(int i=0; i<10; i++) {
                s.onNext(i);
            }
            s.onCompleted();

            //            System.out.println("countParent: " + countParent.get());
            //            System.out.println("countChildren: " + countChildren.get());
            //            System.out.println("countTotal: " + countTotal.get());

            // 9+8+7+6+5+4+3+2+1+0 == 45
            assertEquals(45, list.size());
        }

    }
}
