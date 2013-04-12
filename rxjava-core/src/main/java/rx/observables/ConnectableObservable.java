package rx.observables;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.subjects.DefaultSubject;
import rx.subjects.Subject;
import rx.util.functions.Func1;

public class ConnectableObservable<T, R> extends Observable<T> {
    private final Observable<T> source;
    private final Subject<T, R> subject;

    public static <T, R> ConnectableObservable create(final Observable<T> source, final Subject<T, R> subject) {
        return new ConnectableObservable<T, R>(source, subject, new Func1<Observer<T>, Subscription>() {
            @Override
            public Subscription call(Observer<T> observer) {
                return subject.subscribe(observer);
            }
        });
    }

    protected ConnectableObservable(Observable<T> source, Subject<T, R> subject, Func1<Observer<T>, Subscription> onSubscribe) {
        super(onSubscribe);
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
