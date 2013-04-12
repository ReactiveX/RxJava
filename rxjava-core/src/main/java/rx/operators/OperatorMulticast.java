package rx.operators;

import org.junit.Test;
import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subjects.DefaultSubject;
import rx.subjects.Subject;
import rx.util.functions.Func1;

import static org.mockito.Mockito.*;

public class OperatorMulticast {
    public static <T, R> ConnectableObservable<R> multicast(Observable<T> source, final Subject<T, R> subject) {
        return new MulticastConnectableObservable<T ,R>(source, subject);
    }

    private static class MulticastConnectableObservable<T, R> extends ConnectableObservable<R> {
        private final Observable<T> source;
        private final Subject<T, R> subject;

        public MulticastConnectableObservable(Observable<T> source, final Subject<T, R> subject) {
            super(new Func1<Observer<R>, Subscription>() {
                @Override
                public Subscription call(Observer<R> observer) {
                    return subject.subscribe(observer);
                }
            });
            this.source = source;
            this.subject = subject;
        }

        public Subscription connect() {
            return source.subscribe(new Observer<T>() {
                @Override
                public void onCompleted() {
                    subject.onCompleted();
                }

                @Override
                public void onError(Exception e) {
                    subject.onError(e);
                }

                @Override
                public void onNext(T args) {
                    subject.onNext(args);
                }
            });
        }


    }

    public static class UnitTest {

        @Test
        public void testMulticast() {
            Subscription s = mock(Subscription.class);
            TestObservable source = new TestObservable(s);

            ConnectableObservable<String> multicasted = OperatorMulticast.multicast(source,
                    DefaultSubject.<String>create());

            Observer<String> observer = mock(Observer.class);
            multicasted.subscribe(observer);

            source.sendOnNext("one");
            source.sendOnNext("two");

            multicasted.connect();

            source.sendOnNext("three");
            source.sendOnNext("four");
            source.sendOnCompleted();

            verify(observer, never()).onNext("one");
            verify(observer, never()).onNext("two");
            verify(observer, times(1)).onNext("three");
            verify(observer, times(1)).onNext("four");
            verify(observer, times(1)).onCompleted();

        }


        private static class TestObservable extends Observable<String> {

            Observer<String> observer = new Observer<String>() {
                @Override
                public void onCompleted() {
                    // Do nothing
                }

                @Override
                public void onError(Exception e) {
                    // Do nothing
                }

                @Override
                public void onNext(String args) {
                    // Do nothing
                }
            };
            Subscription s;

            public TestObservable(Subscription s) {
                this.s = s;
            }

            /* used to simulate subscription */
            public void sendOnCompleted() {
                observer.onCompleted();
            }

            /* used to simulate subscription */
            public void sendOnNext(String value) {
                observer.onNext(value);
            }

            /* used to simulate subscription */
            public void sendOnError(Exception e) {
                observer.onError(e);
            }

            @Override
            public Subscription subscribe(final Observer<String> observer) {
                this.observer = observer;
                return s;
            }

        }

    }
}
