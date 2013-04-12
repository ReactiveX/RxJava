package rx.operators;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subjects.Subject;
import rx.util.functions.Func1;

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


}
