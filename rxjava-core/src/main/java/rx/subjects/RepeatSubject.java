package rx.subjects;

import org.junit.Test;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Func1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public final class RepeatSubject<T> extends Subject<T, T>
{

    private boolean isDone = false;
    private Exception exception = null;
    private final Map<Subscription,Observer<T>> subscriptions = new HashMap<Subscription, Observer<T>>();
    private final List<T> history = Collections.synchronizedList(new ArrayList<T>());

    public static <T> RepeatSubject<T> create() {
        return new RepeatSubject<T>(new DelegateSubscriptionFunc<T>());
    }

    private RepeatSubject(DelegateSubscriptionFunc<T> onSubscribe) {
        super(onSubscribe);
        onSubscribe.wrap(new SubscriptionFunc());
    }
    private static final class DelegateSubscriptionFunc<T> implements Func1<Observer<T>,Subscription>
    {
        private Func1<Observer<T>, Subscription> delegate = null;

        public void wrap(Func1<Observer<T>, Subscription> delegate)
        {
            if (this.delegate != null) {
                throw new UnsupportedOperationException("delegate already set");
            }
            this.delegate = delegate;
        }

        @Override
        public Subscription call(Observer<T> observer)
        {
            return delegate.call(observer);
        }
    }

    private class SubscriptionFunc implements Func1<Observer<T>, Subscription>
    {
        @Override
        public Subscription call(Observer<T> observer) {
            int item = 0;
            Subscription subscription;

            for (;;) {
                while (item < history.size()) {
                    observer.onNext(history.get(item++));
                }

                synchronized (subscriptions) {
                    if (item < history.size()) {
                        continue;
                    }

                    if (exception != null) {
                        observer.onError(exception);
                        return Subscriptions.empty();
                    }
                    if (isDone) {
                        observer.onCompleted();
                        return Subscriptions.empty();
                    }

                    subscription = new RepeatSubjectSubscription();
                    subscriptions.put(subscription, observer);
                    break;
                }
            }

            return subscription;
        }
    }

    private class RepeatSubjectSubscription implements Subscription
    {
        @Override
        public void unsubscribe()
        {
            synchronized (subscriptions) {
                subscriptions.remove(this);
            }
        }
    }

    @Override
    public void onCompleted()
    {
        synchronized (subscriptions) {
            isDone = true;
            for (Observer<T> observer : new ArrayList<Observer<T>>(subscriptions.values())) {
                observer.onCompleted();
            }
            subscriptions.clear();
        }
    }

    @Override
    public void onError(Exception e)
    {
        synchronized (subscriptions) {
            if (isDone) {
                return;
            }
            isDone = true;
            exception = e;
            for (Observer<T> observer : new ArrayList<Observer<T>>(subscriptions.values())) {
                observer.onError(e);
            }
            subscriptions.clear();
        }
    }

    @Override
    public void onNext(T args)
    {
        synchronized (subscriptions) {
            history.add(args);
            for (Observer<T> observer : new ArrayList<Observer<T>>(subscriptions.values())) {
                observer.onNext(args);
            }
        }
    }

    public static class UnitTest {

        private final Exception testException = new Exception();

        @Test
        public void testCompleted() {
            RepeatSubject<Object> subject = RepeatSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onCompleted();

            subject.onNext("four");
            subject.onCompleted();
            subject.onError(new Exception());

            assertCompletedObserver(aObserver);

            aObserver = mock(Observer.class);
            subject.subscribe(aObserver);
            assertCompletedObserver(aObserver);
        }

        private void assertCompletedObserver(Observer<String> aObserver)
        {
            verify(aObserver, times(1)).onNext("one");
            verify(aObserver, times(1)).onNext("two");
            verify(aObserver, times(1)).onNext("three");
            verify(aObserver, Mockito.never()).onError(any(Exception.class));
            verify(aObserver, times(1)).onCompleted();
        }

        @Test
        public void testError() {
            RepeatSubject<Object> subject = RepeatSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");
            subject.onNext("three");
            subject.onError(testException);

            subject.onNext("four");
            subject.onError(new Exception());
            subject.onCompleted();

            assertErrorObserver(aObserver);

            aObserver = mock(Observer.class);
            subject.subscribe(aObserver);
            assertErrorObserver(aObserver);
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
            RepeatSubject<Object> subject = RepeatSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");

            assertObservedUntilTwo(aObserver);

            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);
            assertObservedUntilTwo(anotherObserver);

            subject.onNext("three");
            subject.onCompleted();

            assertCompletedObserver(aObserver);
            assertCompletedObserver(anotherObserver);
        }

        @Test
        public void testUnsubscribeFirstObserver() {
            RepeatSubject<Object> subject = RepeatSubject.create();

            Observer<String> aObserver = mock(Observer.class);
            Subscription subscription = subject.subscribe(aObserver);

            subject.onNext("one");
            subject.onNext("two");

            subscription.unsubscribe();
            assertObservedUntilTwo(aObserver);

            Observer<String> anotherObserver = mock(Observer.class);
            subject.subscribe(anotherObserver);
            assertObservedUntilTwo(anotherObserver);

            subject.onNext("three");
            subject.onCompleted();

            assertObservedUntilTwo(aObserver);
            assertCompletedObserver(anotherObserver);
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
        public void testUnsubscribeFromOnNext() {
            RepeatSubject<Object> subject = RepeatSubject.create();

            UnsubscribeTest test1 = UnsubscribeTest.createOnNext(subject);
            UnsubscribeTest test2 = UnsubscribeTest.createOnNext(subject);

            subject.onNext("one");

            test1.assertPassed();
            test2.assertPassed();
        }

        @Test
        public void testUnsubscribeFromOnCompleted() {
            RepeatSubject<Object> subject = RepeatSubject.create();

            UnsubscribeTest test1 = UnsubscribeTest.createOnCompleted(subject);
            UnsubscribeTest test2 = UnsubscribeTest.createOnCompleted(subject);

            subject.onCompleted();

            test1.assertPassed();
            test2.assertPassed();
        }

        @Test
        public void testUnsubscribeFromOnError() {
            RepeatSubject<Object> subject = RepeatSubject.create();

            UnsubscribeTest test1 = UnsubscribeTest.createOnError(subject);
            UnsubscribeTest test2 = UnsubscribeTest.createOnError(subject);

            subject.onError(new Exception());

            test1.assertPassed();
            test2.assertPassed();
        }

        private static class UnsubscribeTest
        {
            private Subscription subscription;

            private UnsubscribeTest() {}

            public static <T> UnsubscribeTest createOnNext(Observable<T> observable)
            {
                final UnsubscribeTest test = new UnsubscribeTest();
                test.setSubscription(observable.subscribe(new Observer<T>()
                {
                    @Override
                    public void onCompleted()
                    {
                    }

                    @Override
                    public void onError(Exception e)
                    {
                    }

                    @Override
                    public void onNext(T args)
                    {
                        test.doUnsubscribe();
                    }
                }));
                return test;
            }

            public static <T> UnsubscribeTest createOnCompleted(Observable<T> observable)
            {
                final UnsubscribeTest test = new UnsubscribeTest();
                test.setSubscription(observable.subscribe(new Observer<T>()
                {
                    @Override
                    public void onCompleted()
                    {
                        test.doUnsubscribe();
                    }

                    @Override
                    public void onError(Exception e)
                    {
                    }

                    @Override
                    public void onNext(T args)
                    {
                    }
                }));
                return test;
            }

            public static <T> UnsubscribeTest createOnError(Observable<T> observable)
            {
                final UnsubscribeTest test = new UnsubscribeTest();
                test.setSubscription(observable.subscribe(new Observer<T>()
                {
                    @Override
                    public void onCompleted()
                    {
                    }

                    @Override
                    public void onError(Exception e)
                    {
                        test.doUnsubscribe();
                    }

                    @Override
                    public void onNext(T args)
                    {
                    }
                }));
                return test;
            }

            private void setSubscription(Subscription subscription)
            {
                this.subscription = subscription;
            }

            private void doUnsubscribe()
            {
                Subscription subscription = this.subscription;
                this.subscription = null;
                subscription.unsubscribe();
            }

            public void assertPassed()
            {
                assertTrue("expected notification was received", subscription == null);
            }
        }
    }
}
